//preciso entender todos os regex e slice replace
export function formatCep(value){

    return value.
    replace(/\D/g, "")
    .slice(0,8)
    .replace(/(\d{5})(\d)/, "$1-$2")

}