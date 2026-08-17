export function getAuthHeader() {

    const token =
        localStorage.getItem("token");

    return {
        Authorization: `Bearer ${token}`
    };
}
