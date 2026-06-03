// Story: US-003
import React, { useRef, useState, useCallback, useId } from 'react';

// ── Constants ─────────────────────────────────────────────────────────────────

const OTP_LENGTH = 6;

// ── Props ─────────────────────────────────────────────────────────────────────

export interface OtpInputProps {
  /**
   * Called on every digit change with the current accumulated OTP string.
   * The string length is 0–OTP_LENGTH; the parent can auto-submit when
   * the length reaches OTP_LENGTH.
   */
  onChange: (otp: string) => void;
  /** Disables all input boxes (e.g. while a verification request is in-flight). */
  disabled?: boolean;
  /**
   * When true, all boxes receive the error visual style (red border).
   * Pair with a sibling error message that has role="alert".
   */
  hasError?: boolean;
}

// ── Component ─────────────────────────────────────────────────────────────────

/**
 * OtpInput
 *
 * Renders OTP_LENGTH individual single-digit input boxes.
 * Behaviour:
 *  - Auto-advance focus to next box when a digit is entered.
 *  - Backspace on an empty box focuses and clears the previous box.
 *  - Arrow-left / Arrow-right navigate between boxes.
 *  - Paste: distributes pasted digits across boxes from left to right.
 *  - onChange fires on every digit change with the joined OTP string.
 *
 * Accessibility:
 *  - role="group" with visually-hidden label via aria-labelledby.
 *  - Each input has aria-label="Digit N of 6".
 *  - aria-invalid={hasError} on every input.
 *  - First input carries autoComplete="one-time-code" for SMS autofill.
 */
export default function OtpInput({
  onChange,
  disabled = false,
  hasError = false,
}: OtpInputProps) {
  const [digits, setDigits] = useState<string[]>(() => Array(OTP_LENGTH).fill(''));
  const inputRefs = useRef<Array<HTMLInputElement | null>>(Array(OTP_LENGTH).fill(null));
  const groupLabelId = useId();

  // ── Helpers ────────────────────────────────────────────────────────────────

  const focusBox = useCallback((index: number) => {
    const target = Math.max(0, Math.min(OTP_LENGTH - 1, index));
    inputRefs.current[target]?.focus();
  }, []);

  /**
   * Commit a new digits array to state and notify the parent.
   * Called on every digit change (partial or complete).
   */
  const commitDigits = useCallback(
    (next: string[]) => {
      setDigits(next);
      onChange(next.join(''));
    },
    [onChange],
  );

  // ── Event handlers ─────────────────────────────────────────────────────────

  const handleChange = (index: number, rawValue: string) => {
    // Accept only the most-recently typed digit (handles browser autocomplete
    // delivering two chars when a box already contains a digit).
    const digit = rawValue.replace(/\D/g, '').slice(-1);
    const next = [...digits];
    next[index] = digit;
    commitDigits(next);

    if (digit && index < OTP_LENGTH - 1) {
      focusBox(index + 1);
    }
  };

  const handleKeyDown = (index: number, e: React.KeyboardEvent<HTMLInputElement>) => {
    if (e.key === 'Backspace') {
      if (digits[index] !== '') {
        // Clear the current box
        const next = [...digits];
        next[index] = '';
        commitDigits(next);
      } else if (index > 0) {
        // Box is already empty: move back and clear the previous box
        const next = [...digits];
        next[index - 1] = '';
        commitDigits(next);
        focusBox(index - 1);
      }
      // Prevent default so the browser's own backspace handling doesn't fire.
      e.preventDefault();
    } else if (e.key === 'ArrowLeft') {
      focusBox(index - 1);
      e.preventDefault();
    } else if (e.key === 'ArrowRight') {
      focusBox(index + 1);
      e.preventDefault();
    }
  };

  const handlePaste = (e: React.ClipboardEvent<HTMLInputElement>) => {
    e.preventDefault();
    const pasted = e.clipboardData.getData('text').replace(/\D/g, '').slice(0, OTP_LENGTH);
    if (!pasted) return;

    const next = [...digits];
    for (let i = 0; i < pasted.length; i++) {
      next[i] = pasted[i];
    }
    commitDigits(next);

    // Move focus to the first unfilled box, or the last box if all filled.
    const nextEmpty = next.findIndex((d) => d === '');
    focusBox(nextEmpty === -1 ? OTP_LENGTH - 1 : nextEmpty);
  };

  // ── Styles ─────────────────────────────────────────────────────────────────

  const inputClass = [
    // Size & shape
    'h-12 w-10 sm:w-12 rounded-md border text-center text-xl font-semibold',
    'shadow-sm transition-colors',
    // Focus ring
    'focus:outline-none focus:ring-2 focus:ring-brand-500 focus:ring-offset-1',
    // Disabled
    'disabled:cursor-not-allowed disabled:bg-gray-100 disabled:text-gray-400',
    // Colour — error vs normal
    hasError
      ? 'border-red-500 bg-red-50 text-red-900 focus:ring-red-400'
      : 'border-gray-300 bg-white text-gray-900 focus:border-brand-500',
  ]
    .filter(Boolean)
    .join(' ');

  // ── Render ─────────────────────────────────────────────────────────────────

  return (
    <div role="group" aria-labelledby={groupLabelId} className="flex flex-col items-center gap-3">
      {/* Visually-hidden group label read by screen readers */}
      <span id={groupLabelId} className="sr-only">
        One-time passcode — enter {OTP_LENGTH} digits
      </span>

      <div className="flex items-center justify-center gap-2 sm:gap-3">
        {digits.map((digit, index) => (
          <input
            // eslint-disable-next-line react/no-array-index-key
            key={index}
            ref={(el) => {
              inputRefs.current[index] = el;
            }}
            type="text"
            inputMode="numeric"
            pattern="[0-9]"
            maxLength={1}
            value={digit}
            disabled={disabled}
            autoComplete={index === 0 ? 'one-time-code' : 'off'}
            aria-label={`Digit ${index + 1} of ${OTP_LENGTH}`}
            aria-invalid={hasError}
            aria-required={index === 0}
            onChange={(e) => handleChange(index, e.target.value)}
            onKeyDown={(e) => handleKeyDown(index, e)}
            onPaste={handlePaste}
            // Select existing content on focus so a new digit replaces it.
            onFocus={(e) => e.target.select()}
            className={inputClass}
          />
        ))}
      </div>
    </div>
  );
}
