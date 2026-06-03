// Story: US-001

interface Rule {
  label: string;
  met: boolean;
}

interface PasswordChecklistProps {
  password: string;
}

function buildRules(password: string): Rule[] {
  return [
    { label: 'At least 8 characters', met: password.length >= 8 },
    { label: 'An uppercase letter (A–Z)', met: /[A-Z]/.test(password) },
    { label: 'A lowercase letter (a–z)', met: /[a-z]/.test(password) },
    { label: 'A number (0–9)', met: /[0-9]/.test(password) },
    {
      label: 'A special character (! ? @ # etc.)',
      met: /[!?@#$%^&*()\-_=+[\]{};:'",.<>/\\|`~]/.test(password),
    },
  ];
}

/**
 * Live password complexity checklist.
 * Each rule shows a ✓ (green) or ✗ (red) icon + text — not colour alone.
 * Screen-reader accessible via aria-live="polite".
 */
export default function PasswordChecklist({ password }: PasswordChecklistProps) {
  const rules = buildRules(password);

  return (
    <ul
      aria-label="Password requirements"
      aria-live="polite"
      className="mt-1 space-y-1"
    >
      {rules.map((rule) => (
        <li
          key={rule.label}
          className={`flex items-center gap-2 text-xs ${
            rule.met ? 'text-green-700' : 'text-red-600'
          }`}
        >
          {rule.met ? (
            /* Check icon */
            <svg
              aria-label="Met"
              className="h-3.5 w-3.5 flex-shrink-0"
              viewBox="0 0 20 20"
              fill="currentColor"
            >
              <path
                fillRule="evenodd"
                d="M16.704 4.153a.75.75 0 0 1 .143 1.052l-8 10.5a.75.75 0 0 1-1.127.075l-4.5-4.5a.75.75 0 0 1 1.06-1.06l3.894 3.893 7.48-9.817a.75.75 0 0 1 1.05-.143Z"
                clipRule="evenodd"
              />
            </svg>
          ) : (
            /* Cross icon */
            <svg
              aria-label="Not met"
              className="h-3.5 w-3.5 flex-shrink-0"
              viewBox="0 0 20 20"
              fill="currentColor"
            >
              <path d="M6.28 5.22a.75.75 0 0 0-1.06 1.06L8.94 10l-3.72 3.72a.75.75 0 1 0 1.06 1.06L10 11.06l3.72 3.72a.75.75 0 1 0 1.06-1.06L11.06 10l3.72-3.72a.75.75 0 0 0-1.06-1.06L10 8.94 6.28 5.22Z" />
            </svg>
          )}
          <span>{rule.label}</span>
        </li>
      ))}
    </ul>
  );
}
