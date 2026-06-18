document.addEventListener("DOMContentLoaded", () => {
    const formatPhone = (rawValue) => {
        const digits = rawValue.replace(/\D/g, "").slice(0, 11);
        if (digits.length > 10) {
            return digits.replace(/^(\d{2})(\d{5})(\d{0,4}).*/, "($1) $2-$3");
        }
        if (digits.length > 6) {
            return digits.replace(/^(\d{2})(\d{4})(\d{0,4}).*/, "($1) $2-$3");
        }
        if (digits.length > 2) {
            return digits.replace(/^(\d{2})(\d{0,5}).*/, "($1) $2");
        }
        if (digits.length > 0) {
            return digits.replace(/^(\d*)/, "($1");
        }
        return "";
    };

    const phoneInputs = document.querySelectorAll("[data-phone]");
    phoneInputs.forEach((input) => {
        const applyMask = () => {
            input.value = formatPhone(input.value);
        };

        input.addEventListener("input", applyMask);
        input.addEventListener("blur", applyMask);
        input.addEventListener("paste", () => {
            requestAnimationFrame(applyMask);
        });

        applyMask();
    });
});
