package app.k9mail.feature.account.setup.domain.usecase

import app.k9mail.feature.account.setup.domain.DomainContract.UseCase
import net.thunderbird.core.common.mail.EmailAddressParserError
import net.thunderbird.core.common.mail.EmailAddressParserException
import net.thunderbird.core.common.mail.toEmailAddressOrNull
import net.thunderbird.core.common.mail.toUserEmailAddress
import net.thunderbird.core.outcome.Outcome
import net.thunderbird.core.validation.ValidationError
import net.thunderbird.core.validation.ValidationOutcome
import net.thunderbird.core.validation.ValidationSuccess
import net.thunderbird.legacy.logging.Log

/**
 * Validate an email address that the user wants to add to an account.
 *
 * This only allows a subset of all valid email addresses. We currently don't support international email addresses
 * and don't allow quoted local parts, or email addresses exceeding length restrictions.
 *
 * Note: Do NOT use this to validate recipients in incoming or outgoing messages. Use [String.toEmailAddressOrNull]
 * instead.
 *
 * An address that is already set up as an account is rejected here, at the first step of setup,
 * so the user is told before any server settings are entered or any account state is written.
 * [existingEmailAddresses] supplies the addresses already in use; it defaults to none, which
 * leaves the validation exactly as it was.
 */
class ValidateEmailAddress(
    private val existingEmailAddresses: () -> Collection<String> = { emptyList() },
) : UseCase.ValidateEmailAddress {

    override fun execute(emailAddress: String): ValidationOutcome {
        rejectBeforeParsing(emailAddress)?.let { return Outcome.Failure(it) }

        return try {
            val parsedEmailAddress = emailAddress.toUserEmailAddress()

            if (parsedEmailAddress.warnings.isEmpty()) {
                ValidationSuccess
            } else {
                Outcome.Failure(ValidateEmailAddressError.NotAllowed)
            }
        } catch (e: EmailAddressParserException) {
            Log.v(e, "Error parsing email address: %s", emailAddress)

            val validationError = when (e.error) {
                EmailAddressParserError.AddressLiteralsNotSupported,
                EmailAddressParserError.LocalPartLengthExceeded,
                EmailAddressParserError.DnsLabelLengthExceeded,
                EmailAddressParserError.DomainLengthExceeded,
                EmailAddressParserError.TotalLengthExceeded,
                EmailAddressParserError.QuotedStringInLocalPart,
                EmailAddressParserError.LocalPartRequiresQuotedString,
                EmailAddressParserError.EmptyLocalPart,
                -> {
                    ValidateEmailAddressError.NotAllowed
                }

                else -> {
                    if ('@' in emailAddress) {
                        // We currently don't support or recognize international email addresses. So if the string
                        // contains an "@" character, we assume it's a valid email address that we don't support.
                        ValidateEmailAddressError.InvalidOrNotSupported
                    } else {
                        ValidateEmailAddressError.InvalidEmailAddress
                    }
                }
            }

            Outcome.Failure(validationError)
        }
    }

    /**
     * The reasons an address can be turned down without parsing it at all.
     *
     * A duplicate is one of them: a padded or differently cased duplicate would otherwise be
     * reported as a malformed address rather than as the duplicate it is.
     */
    private fun rejectBeforeParsing(emailAddress: String): ValidateEmailAddressError? = when {
        emailAddress.isBlank() -> ValidateEmailAddressError.EmptyEmailAddress
        isAlreadyAdded(emailAddress) -> ValidateEmailAddressError.AlreadyAdded
        else -> null
    }

    /**
     * Whether this address is already set up as an account.
     *
     * The comparison is on the address as a person would read it: surrounding whitespace is
     * ignored, and case is not significant. " User@Example.COM " and "user@example.com" are the
     * same account, so the second one is refused.
     */
    private fun isAlreadyAdded(emailAddress: String): Boolean {
        val candidate = emailAddress.normalizedForComparison()

        return existingEmailAddresses().any { it.normalizedForComparison() == candidate }
    }

    private fun String.normalizedForComparison(): String = trim().lowercase()

    sealed interface ValidateEmailAddressError : ValidationError {
        data object EmptyEmailAddress : ValidateEmailAddressError
        data object NotAllowed : ValidateEmailAddressError
        data object InvalidOrNotSupported : ValidateEmailAddressError
        data object InvalidEmailAddress : ValidateEmailAddressError
        data object AlreadyAdded : ValidateEmailAddressError
    }
}
