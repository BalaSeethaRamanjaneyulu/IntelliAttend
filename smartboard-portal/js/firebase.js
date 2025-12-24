import { initializeApp } from "firebase/app";
import {
    getAuth,
    createUserWithEmailAndPassword,
    signInWithEmailAndPassword,
    signOut,
    onAuthStateChanged
} from "firebase/auth";

// Your web app's Firebase configuration
const firebaseConfig = {
    apiKey: "AIzaSyBooFadQf3TZFvZOUJkihMUdgexrbeoQnE",
    authDomain: "intelliattend-a2564.firebaseapp.com",
    projectId: "intelliattend-a2564",
    storageBucket: "intelliattend-a2564.firebasestorage.app",
    messagingSenderId: "738499328288",
    appId: "1:738499328288:web:c345f44de9d8393062ff45",
    measurementId: "G-L9E2NMFTGB"
};

// Initialize Firebase
const app = initializeApp(firebaseConfig);
const auth = getAuth(app);

/**
 * Register a new user with email and password
 * @param {string} email 
 * @param {string} password 
 * @returns {Promise}
 */
export const registerUser = async (email, password) => {
    try {
        const userCredential = await createUserWithEmailAndPassword(auth, email, password);
        console.log("Signed up:", userCredential.user);
        return userCredential.user;
    } catch (error) {
        console.error("Sign up error:", error.code, error.message);
        throw error;
    }
};

/**
 * Sign in an existing user
 * @param {string} email 
 * @param {string} password 
 * @returns {Promise}
 */
export const loginUser = async (email, password) => {
    try {
        const userCredential = await signInWithEmailAndPassword(auth, email, password);
        console.log("Signed in:", userCredential.user);
        return userCredential.user;
    } catch (error) {
        console.error("Sign in error:", error.code, error.message);
        throw error;
    }
};

/**
 * Sign out the current user
 * @returns {Promise}
 */
export const logoutUser = async () => {
    try {
        await signOut(auth);
        console.log("User signed out.");
    } catch (error) {
        console.error("Sign out error:", error);
        throw error;
    }
};

/**
 * Monitor authentication state changes
 * @param {Function} callback - Function to call with the user object (or null)
 */
export const monitorAuthState = (callback) => {
    onAuthStateChanged(auth, (user) => {
        if (user) {
            console.log("User is signed in:", user);
        } else {
            console.log("User is signed out.");
        }
        if (typeof callback === 'function') {
            callback(user);
        }
    });
};

export { auth };
