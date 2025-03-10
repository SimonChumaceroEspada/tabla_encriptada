import axios from "axios";

const API_URL = "http://localhost:3003/api";

export const api = axios.create({
    baseURL: API_URL,
});
