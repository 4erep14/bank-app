// Story: US-001
import RegistrationForm from '../components/RegistrationForm';

/**
 * Route container for /register.
 * Renders the page chrome (heading, subtitle) around RegistrationForm.
 */
export default function RegistrationPage() {
  return (
    <main
      className="flex min-h-screen items-start justify-center bg-gray-50 px-4 py-12 sm:items-center"
      aria-labelledby="register-heading"
    >
      <div className="w-full max-w-[560px] rounded-xl bg-white p-6 shadow-md sm:p-8">
        {/* Page header */}
        <div className="mb-6">
          <h1
            id="register-heading"
            className="text-2xl font-bold text-gray-900"
          >
            Create your account
          </h1>
          <p className="mt-1 text-sm text-gray-500">
            Join NorthBank in a couple of minutes.
          </p>
        </div>

        <RegistrationForm />
      </div>
    </main>
  );
}
