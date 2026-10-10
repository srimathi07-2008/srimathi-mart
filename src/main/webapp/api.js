// ==============================
// SRIMATHI MART - SERVER BRIDGE
//
// This file is NEW. It was added during the Java conversion and holds every
// call to the Java servlets in one place, so that script.js could keep its
// original structure and only have its data source swapped out.
//
// Nothing here touches styling, layout or markup structure. The render
// helpers below rebuild the exact same elements and class names the original
// static HTML used, so the pages look the same as before.
// ==============================


// ==============================
// CONTEXT PATH
//
// The app is deployed at /srimathi-mart, not at the server root, so every
// fetch has to be prefixed. This works out the prefix from the current URL
// instead of hardcoding it, so the same build runs at any context path.
// ==============================

const SM_CONTEXT = (function () {

    const path = window.location.pathname;
    const firstSlash = path.indexOf("/", 1);

    if (firstSlash === -1) {
        return "";
    }

    const candidate = path.substring(0, firstSlash);

    // A single-segment path like /home.html means we are at the root.
    return candidate.includes(".") ? "" : candidate;
})();


function smUrl(path) {
    return SM_CONTEXT + path;
}


// ==============================
// FETCH HELPERS
//
// X-Requested-With tells AuthFilter to answer with JSON instead of
// redirecting to a login page, which keeps error handling predictable.
// ==============================

async function smGet(path) {

    const response = await fetch(smUrl(path), {
        method: "GET",
        headers: {
            "X-Requested-With": "XMLHttpRequest"
        },
        credentials: "same-origin"
    });

    return smParse(response);
}


async function smPost(path, fields) {

    const body = new URLSearchParams();

    Object.keys(fields || {}).forEach(function (key) {
        if (fields[key] !== undefined && fields[key] !== null) {
            body.append(key, fields[key]);
        }
    });

    const response = await fetch(smUrl(path), {
        method: "POST",
        headers: {
            "Content-Type": "application/x-www-form-urlencoded; charset=UTF-8",
            "X-Requested-With": "XMLHttpRequest"
        },
        credentials: "same-origin",
        body: body.toString()
    });

    return smParse(response);
}


async function smParse(response) {

    let data = {};

    try {
        data = await response.json();
    } catch (error) {
        data = {
            ok: false,
            error: "The server sent an unexpected response."
        };
    }

    if (!response.ok || data.ok === false) {
        const error = new Error(data.error || "Request failed.");
        error.status = response.status;
        throw error;
    }

    return data;
}


// ==============================
// PRICE FORMATTING
//
// The original pages printed prices as "₹1,499". The server sends a plain
// number, so this puts the rupee sign and grouping back exactly as before.
// ==============================

function smFormatPrice(amount) {

    const value = Number(amount) || 0;

    return "\u20B9" + value.toLocaleString("en-IN", {
        minimumFractionDigits: value % 1 === 0 ? 0 : 2,
        maximumFractionDigits: 2
    });
}


// ==============================
// HTML ESCAPING
//
// Product names and descriptions now come from the database, which means a
// seller controls them. Everything is escaped before it reaches innerHTML.
// ==============================

function smEscape(text) {

    return String(text === undefined || text === null ? "" : text)
        .replace(/&/g, "&amp;")
        .replace(/</g, "&lt;")
        .replace(/>/g, "&gt;")
        .replace(/"/g, "&quot;")
        .replace(/'/g, "&#39;");
}


// ==============================
// PRODUCT IMAGE
//
// The original cards used an emoji glyph inside .product-image. Sellers can
// now also supply a real image URL, so both are supported: a value that looks
// like a URL renders as an <img>, anything else is printed as-is, which keeps
// the existing emoji products looking exactly as they did.
// ==============================

function smProductImage(imageUrl) {
    const value = String(imageUrl || "").trim();

    if (/^(https?:)?\/\//i.test(value) || value.startsWith("/")) {
        return '<img src="' + smEscape(value) + '" alt="Product image" ' +
               'style="width:100%;height:100%;object-fit:contain;">';
    }

    if (value) {
        return '<span>' + smEscape(value) + '</span>';
    }

    return '<span>\uD83D\uDECD\uFE0F</span>';
}


// ==============================
// SESSION
// ==============================

let smCurrentUser = null;

async function smLoadSession() {

    try {
        const data = await smGet("/api/session");
        smCurrentUser = data.authenticated ? data.user : null;
    } catch (error) {
        smCurrentUser = null;
    }

    return smCurrentUser;
}


function smLogout(event) {

    if (event) {
        event.preventDefault();
    }

    window.location.href = smUrl("/api/logout");
}


// ==============================
// PRODUCT RENDERING
//
// Rebuilds the same <article class="product-card"> structure the original
// home.html contained, field for field, so style.css needs no changes.
// ==============================

function smRenderProducts(products) {

    const grid = document.getElementById("productGrid");

    if (!grid) {
        return;
    }

    if (!products || products.length === 0) {

        grid.innerHTML =
            '<p style="grid-column:1/-1;text-align:center;padding:40px 0;">' +
            'No products found.</p>';

        return;
    }

    const wishlist =
        JSON.parse(localStorage.getItem("srimathiWishlist")) || [];

    grid.innerHTML = products.map(function (product) {
        console.log("Product:", product.name, "Image:", product.imageUrl);

        const inWishlist =
            wishlist.some(item => item.name === product.name);

        return `
                <article class="product-card" data-product-id="${product.id}">

                    <div class="product-image">
                       ${smProductImage(
    product.name.toLowerCase().includes("decorative cushion")
        ? "/srimathi-mart/images/decorative-cushion.jpg"
        : product.imageUrl
)}
                    </div>

                    <div class="product-info">

                        <span class="product-category">
                            ${smEscape(product.category)}
                        </span>

                        <h3>
                            ${smEscape(product.name)}
                        </h3>

                        <p>
                            ${smEscape(product.description)}
                        </p>

                        <div class="product-bottom">

                            <strong>
                                ${smFormatPrice(product.price)}
                            </strong>

                            <span>
                                ${product.stockQuantity > 0
                                    ? "In stock"
                                    : "Out of stock"}
                            </span>

                        </div>
<button class="product-wishlist" onclick="toggleProductWishlist(this)">
    ${inWishlist ? "&#9829;" : "&#9825;"}
</button>
                     <button class="add-cart-button" onclick="addToCart(this)"
                             ${product.stockQuantity > 0 ? "" : "disabled"}>
    ${product.stockQuantity > 0 ? "Add to Cart" : "Out of Stock"}
</button>

                    </div>

                </article>`;

    }).join("");
}


// ==============================
// CATALOGUE LOADING
//
// Search and category filtering now run in SQL rather than by hiding DOM
// nodes, so they work across the whole catalogue instead of only the handful
// of cards that happen to be on the page.
// ==============================

let smActiveCategory = "";
let smActiveKeyword = "";


async function smLoadProducts() {

    const grid = document.getElementById("productGrid");

    if (!grid) {
        return;
    }

    try {

        const query = new URLSearchParams();

        if (smActiveKeyword) {
            query.append("q", smActiveKeyword);
        }

        if (smActiveCategory) {
            query.append("category", smActiveCategory);
        }

        const suffix = query.toString() ? "?" + query.toString() : "";
        const data = await smGet("/api/products" + suffix);

        smRenderProducts(data.products);

    } catch (error) {

        grid.innerHTML =
            '<p style="grid-column:1/-1;text-align:center;padding:40px 0;">' +
            smEscape(error.message) + "</p>";
    }
}


// ==============================
// CART COUNT BADGE
// ==============================

async function smRefreshCartCount() {

    const badge = document.getElementById("cartCount");

    if (!badge) {
        return;
    }

    try {
        const data = await smGet("/api/cart");
        badge.textContent = data.totalQuantity;
    } catch (error) {
        // Signed out buyers simply see zero, as before.
        badge.textContent = "0";
    }
}
