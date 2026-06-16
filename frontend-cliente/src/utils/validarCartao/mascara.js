

export function formatCardNumber(value) {

    return value
        .replace(/\D/g, "")
        .slice(0, 16)
        .replace(/(\d{4})(?=\d)/g, "$1 ");



}

export function formatExpiry(value) {

    const numbers = value.replace(/\D/g, "").slice(0, 4);

    if (numbers.length <= 2) return numbers

    const format = numbers.replace(/(\d{2})(\d{1,2})/, "$1/$2");

    return format
}

export function formatCVV(value) {
    return value.replace(/\D/g, "").slice(0, 3);
}

export function formatName(value) {
    return value.replace(/[^a-zA-Z\s]/g, ""); 
}