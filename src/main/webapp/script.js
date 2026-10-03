// ==============================
// PAGE NAVIGATION
// ==============================

function goToPage(page) {
    window.location.href = page;
}


// ==============================
// BUYER LOGIN
// ==============================

const togglePassword =
    document.getElementById("togglePassword");

const passwordInput =
    document.getElementById("password");

if (togglePassword && passwordInput) {

    togglePassword.addEventListener("click", function () {

        if (passwordInput.type === "password") {
            passwordInput.type = "text";
            togglePassword.textContent = "Hide";
        } else {
            passwordInput.type = "password";
            togglePassword.textContent = "Show";
        }

    });

}


const buyerLoginForm =
    document.getElementById("buyerLoginForm");

if (buyerLoginForm) {

    buyerLoginForm.addEventListener("submit", function(event) {

        event.preventDefault();

        const email =
            document.getElementById("email").value.trim();

        const password =
            document.getElementById("password").value.trim();

        if (email === "" || password === "") {

            alert("Please enter your email/phone and password.");

            return;
        }

        smPost("/api/login", {
            email: email,
            password: password,
            role: "BUYER"
        }).then(function (data) {

            window.location.href = data.redirect;

        }).catch(function (error) {

            alert(error.message);

        });

    });

}


function forgotPassword(event) {

    event.preventDefault();

    alert("Password recovery is a demo feature for now.");

}


function demoSocialLogin(provider) {

    alert(provider + " login is a demo feature for now.");

}


function signupDemo(event) {

    event.preventDefault();

    window.location.href = "register.html";

}


// ==============================
// SELLER LOGIN
// ==============================

const sellerLoginForm =
    document.getElementById("sellerLoginForm");

const sellerPassword =
    document.getElementById("sellerPassword");

const sellerTogglePassword =
    document.getElementById("sellerTogglePassword");


if (sellerTogglePassword && sellerPassword) {

    sellerTogglePassword.addEventListener("click", function() {

        if (sellerPassword.type === "password") {
            sellerPassword.type = "text";
            sellerTogglePassword.textContent = "Hide";
        } else {
            sellerPassword.type = "password";
            sellerTogglePassword.textContent = "Show";
        }

    });

}


if (sellerLoginForm) {

    sellerLoginForm.addEventListener("submit", function(event) {

        event.preventDefault();

        const email =
            document.getElementById("sellerEmail").value.trim();

        const password =
            document.getElementById("sellerPassword").value.trim();

        if (email === "" || password === "") {

            alert("Please enter your seller email and password.");

            return;
        }

        smPost("/api/login", {
            email: email,
            password: password,
            role: "SELLER"
        }).then(function (data) {

            window.location.href = data.redirect;

        }).catch(function (error) {

            alert(error.message);

        });

    });

}


function sellerForgotPassword(event) {

    event.preventDefault();

    alert("Seller password recovery is a demo feature for now.");

}


// ==============================
// ADMIN LOGIN
// ==============================

const adminLoginForm =
    document.getElementById("adminLoginForm");

const adminPassword =
    document.getElementById("adminPassword");

const adminTogglePassword =
    document.getElementById("adminTogglePassword");


if (adminTogglePassword && adminPassword) {

    adminTogglePassword.addEventListener("click", function() {

        if (adminPassword.type === "password") {
            adminPassword.type = "text";
            adminTogglePassword.textContent = "Hide";
        } else {
            adminPassword.type = "password";
            adminTogglePassword.textContent = "Show";
        }

    });

}


if (adminLoginForm) {

    adminLoginForm.addEventListener("submit", function(event) {

        event.preventDefault();

        const email =
            document.getElementById("adminEmail").value.trim();

        const password =
            document.getElementById("adminPassword").value.trim();

        if (email === "" || password === "") {

            alert("Please enter your admin email and password.");

            return;
        }

        smPost("/api/login", {
            email: email,
            password: password,
            role: "ADMIN"
        }).then(function (data) {

            window.location.href = data.redirect;

        }).catch(function (error) {

            alert(error.message);

        });

    });

}


function adminForgotPassword(event) {

    event.preventDefault();

    alert("Admin password recovery is a demo feature for now.");

}


// ==============================
// PRODUCT SEARCH
// ==============================

function searchProducts() {

    const searchInput =
        document.getElementById("productSearch");

    if (!searchInput) {
        return;
    }

    smActiveKeyword = searchInput.value.trim();

    smLoadProducts();

}


const productSearch =
    document.getElementById("productSearch");

if (productSearch) {

    productSearch.addEventListener("keydown", function(event) {

        if (event.key === "Enter") {

            searchProducts();

        }

    });

}


// ==============================
// ADD TO CART
// ==============================

function addToCart(button) {

    const productCard =
        button.closest(".product-card");

    if (!productCard) {
        return;
    }

    const productId =
        productCard.dataset.productId;

    if (!productId) {
        return;
    }

    smPost("/api/cart", {
        action: "add",
        productId: productId,
        quantity: 1
    }).then(function (data) {

        button.textContent = "Added ✓";
        button.disabled = true;

        const cartCount =
            document.getElementById("cartCount");

        if (cartCount) {
            cartCount.textContent = data.totalQuantity;
        }

        alert("Product added to cart!");

    }).catch(function (error) {

        if (error.status === 401) {

            alert("Please sign in to add items to your cart.");
            window.location.href = "login.html";

            return;
        }

        alert(error.message);

    });

}


// ==============================
// CART BUTTON
// ==============================

const cartButton =
    document.getElementById("cartButton");

if (cartButton) {

    cartButton.addEventListener("click", function() {

        window.location.href = "cart.html";

    });

}


// ==============================
// PRODUCT WISHLIST
// ==============================

function toggleProductWishlist(button) {

    let wishlist =
        JSON.parse(localStorage.getItem("srimathiWishlist")) || [];

    const productCard =
        button.closest(".product-card");

    if (!productCard) {
        return;
    }

    const productName =
        productCard.querySelector("h3").textContent.trim();

    const existingIndex =
        wishlist.findIndex(
            product => product.name === productName
        );

    if (existingIndex === -1) {

        wishlist.push({
            name: productName
        });

        button.textContent = "\u2665";

        alert("Product added to wishlist!");

    } else {

        wishlist.splice(existingIndex, 1);

        button.textContent = "\u2661";

        alert("Product removed from wishlist.");

    }

    localStorage.setItem(
        "srimathiWishlist",
        JSON.stringify(wishlist)
    );

}


// ==============================
// DISPLAY CART
// ==============================

function displayCart() {

    const cartProductList =
        document.getElementById("cartProductList");

    const emptyCart =
        document.getElementById("emptyCart");

    if (!cartProductList) {
        return;
    }

    smGet("/api/cart").then(function (data) {

        smRenderCart(data);

    }).catch(function (error) {

        if (error.status === 401) {

            if (emptyCart) {
                emptyCart.style.display = "block";
            }

            return;
        }

        cartProductList.innerHTML =
            "<p>" + smEscape(error.message) + "</p>";

    });

}


function smRenderCart(data) {

    const cartProductList =
        document.getElementById("cartProductList");

    const emptyCart =
        document.getElementById("emptyCart");

    if (!cartProductList) {
        return;
    }

    const items =
        data.items || [];

    cartProductList.innerHTML = "";


    if (items.length === 0) {

        if (emptyCart) {
            emptyCart.style.display = "block";
        }

        smUpdateCartSummary(data);

        return;

    }


    if (emptyCart) {
        emptyCart.style.display = "none";
    }


    items.forEach(function(product) {

        const productItem =
            document.createElement("div");

        productItem.className =
            "cart-product-item";


        productItem.innerHTML = `
            <div class="cart-product-icon">
                ${smProductImage(product.imageUrl)}
            </div>

            <div class="cart-product-details">

                <h3>${smEscape(product.name)}</h3>

                <p>${smFormatPrice(product.unitPrice)}</p>

                <div class="quantity-control">

                    <button
                        onclick="changeQuantity(${product.productId}, -1)">
                        -
                    </button>

                    <span>${product.quantity}</span>

                    <button
                        onclick="changeQuantity(${product.productId}, 1)">
                        +
                    </button>

                </div>

                <button
                    class="remove-button"
                    onclick="removeFromCart(${product.productId})">
                    Remove
                </button>

            </div>
        `;

        cartProductList.appendChild(productItem);

    });

    smUpdateCartSummary(data);

}


function smUpdateCartSummary(data) {

    const summaryItems =
        document.getElementById("summaryItems");

    const cartSubtotal =
        document.getElementById("cartSubtotal");

    const cartTotal =
        document.getElementById("cartTotal");


    if (summaryItems) {

        summaryItems.textContent =
            data.totalQuantity;

    }


    if (cartSubtotal) {

        cartSubtotal.textContent =
            smFormatPrice(data.subtotal);

    }


    if (cartTotal) {

        cartTotal.textContent =
            smFormatPrice(data.total);

    }


    const cartCount =
        document.getElementById("cartCount");

    if (cartCount) {

        cartCount.textContent =
            data.totalQuantity;

    }

}


// ==============================
// REMOVE FROM CART
// ==============================

function removeFromCart(productId) {

    smPost("/api/cart", {
        action: "remove",
        productId: productId
    }).then(function (data) {

        smRenderCart(data);

    }).catch(function (error) {

        alert(error.message);

    });

}


// ==============================
// CHANGE QUANTITY
// ==============================

function changeQuantity(productId, change) {

    const row =
        Array.from(
            document.querySelectorAll(".cart-product-item")
        ).find(function(item) {

            const button =
                item.querySelector(".remove-button");

            return button &&
                button.getAttribute("onclick")
                    .includes("(" + productId + ")");

        });


    let current = 1;

    if (row) {

        current =
            parseInt(
                row.querySelector(
                    ".quantity-control span"
                ).textContent,
                10
            ) || 1;

    }


    let next =
        current + change;


    if (next < 1) {
        next = 1;
    }


    smPost("/api/cart", {
        action: "update",
        productId: productId,
        quantity: next
    }).then(function (data) {

        smRenderCart(data);

    }).catch(function (error) {

        alert(error.message);

    });

}


// ==============================
// LOAD CART AFTER PAGE LOADS
// ==============================

document.addEventListener(
    "DOMContentLoaded",
    function() {

        displayCart();

        smRefreshCartCount();


        const wishlist =
            JSON.parse(
                localStorage.getItem("srimathiWishlist")
            ) || [];


        document
            .querySelectorAll(".product-card")
            .forEach(function(product) {

                const button =
                    product.querySelector(
                        ".product-wishlist"
                    );

                if (!button) {
                    return;
                }


                const name =
                    product.querySelector(
                        "h3"
                    ).textContent.trim();


                const exists =
                    wishlist.some(
                        item => item.name === name
                    );


                if (exists) {

                    button.textContent =
                        "\u2665";

                }

            });

    }
);


// ==============================
// CHECKOUT
// ==============================

const checkoutButton =
    document.querySelector(".checkout-button");

if (checkoutButton) {

    checkoutButton.addEventListener(
        "click",
        function() {

            checkoutButton.disabled = true;

            smPost("/api/checkout", {
    paymentMethod: document.querySelector('input[name="paymentMethod"]:checked').value
})
.then(function(data) {

    const paymentMethod =
        document.querySelector(
            'input[name="paymentMethod"]:checked'
        ).value;

    window.location.href =
        "order-confirmation.html?ref=" +
        encodeURIComponent(data.order.paymentRef) +
        "&id=" +
        encodeURIComponent(data.order.id) +
        "&total=" +
        encodeURIComponent(data.order.totalAmount) +
        "&method=" +
        encodeURIComponent(paymentMethod);

})
                .catch(function(error) {

                    checkoutButton.disabled = false;

                    if (error.status === 401) {

                        alert(
                            "Please sign in to check out."
                        );

                        window.location.href =
                            "login.html";

                        return;
                    }

                    alert(error.message);

                });

        }
    );

}


// ==============================
// CATEGORY FILTER
// ==============================

function filterProducts(category) {

    const products =
        document.querySelectorAll(".product-card");


    products.forEach(function(product) {

        const productCategory =
            product.querySelector(
                ".product-category"
            ).textContent.trim();


        if (category === "Home Decor") {

            if (productCategory === "Home Decor") {

                product.style.display = "block";

            } else {

                product.style.display = "none";

            }

        } else {

            product.style.display = "block";

        }

    });


    const productsSection =
        document.getElementById("products");

    if (productsSection) {

        productsSection.scrollIntoView({
            behavior: "smooth"
        });

    }

}


function filterProducts(category) {

    const products = document.querySelectorAll(".product-card");

    products.forEach(function(product) {

        const productCategory =
            product.querySelector(".product-category")
            .textContent
            .trim();

        if (category === "Home Decor") {

            product.style.display =
                productCategory === "Home Decor" ? "block" : "none";

        } else if (category === "Lighting") {

            product.style.display =
                productCategory === "Lighting" ? "block" : "none";

        } else if (category === "Plants") {

            product.style.display =
                product.querySelector("h3")
                    .textContent
                    .toLowerCase()
                    .includes("plant")
                    ? "block"
                    : "none";

        } else {

            product.style.display = "none";
        }

    });

    const productsSection =
        document.getElementById("collections");

    if (productsSection) {

        productsSection.scrollIntoView({
            behavior: "smooth"
        });

    }
}


// ==============================
// ENQUIRY
// ==============================

function showEnquiryForm() {

    const form =
        document.getElementById(
            "enquiryForm"
        );

    if (form) {

        form.style.display =
            "block";

    }

}


function sendEnquiry() {

    const name =
        document.getElementById(
            "enquiryName"
        ).value.trim();


    const email =
        document.getElementById(
            "enquiryEmail"
        ).value.trim();


    const message =
        document.getElementById(
            "enquiryMessage"
        ).value.trim();


    if (
        name === "" ||
        email === "" ||
        message === ""
    ) {

        alert(
            "Please fill all the fields."
        );

        return;

    }


    const phone =
        document.getElementById(
            "enquiryPhone"
        ).value.trim();


    if (phone.length !== 10) {

        alert(
            "Please enter a valid 10-digit phone number."
        );

        return;

    }


    alert(
        "Thank you! Your enquiry has been received."
    );


    document.getElementById(
        "enquiryName"
    ).value = "";


    document.getElementById(
        "enquiryEmail"
    ).value = "";


    document.getElementById(
        "enquiryMessage"
    ).value = "";


    document.getElementById(
        "enquiryPhone"
    ).value = "";

}


// ==============================
// SHOW WISHLIST
// ==============================

function showWishlist() {

    const wishlist =
        JSON.parse(
            localStorage.getItem(
                "srimathiWishlist"
            )
        ) || [];


    const productGrid =
        document.getElementById(
            "productGrid"
        );


    if (!productGrid) {
        return;
    }


    if (wishlist.length === 0) {

        productGrid.innerHTML = `
            <p style="grid-column: 1 / -1; text-align: center;">
                No items in your wishlist yet \u2661
            </p>
        `;


        document
            .getElementById("collections")
            .scrollIntoView({
                behavior: "smooth"
            });


        return;

    }


    const allProducts =
        Array.from(
            document.querySelectorAll(
                ".product-card"
            )
        );


    allProducts.forEach(function(card) {

        const name =
            card.querySelector(
                "h3"
            ).textContent.trim();


        const exists =
            wishlist.some(
                item => item.name === name
            );


        card.style.display =
            exists ? "" : "none";

    });


    document
        .getElementById("collections")
        .scrollIntoView({
            behavior: "smooth"
        });

}
async function loadAdminDashboard() {
    try {
        const response = await fetch("api/admin/dashboard", {
            method: "GET",
            credentials: "same-origin"
        });

        const data = await response.json();

        if (!response.ok) {
            console.error(data);
            return;
        }

        const cards = document.querySelectorAll(".admin-stat-card strong");

        if (cards.length >= 4) {
            cards[0].textContent = data.users;
            cards[1].textContent = data.sellers;
            cards[2].textContent = data.products;
            cards[3].textContent = data.orders;
        }

    } catch (error) {
        console.error("Admin dashboard error:", error);
    }
}
async function loadAdminUsers() {
    try {
        const response = await fetch("api/admin/users", {
            method: "GET",
            credentials: "same-origin"
        });

        const data = await response.json();

        if (!response.ok) {
            console.error(data);
            return;
        }

        const table = document.querySelector(".admin-table");

        if (!table) return;

        const rows = table.querySelectorAll(".admin-table-row:not(.admin-table-head)");

        rows.forEach(row => row.remove());

        data.users.forEach(user => {
            const row = document.createElement("div");
            row.className = "admin-table-row";

            row.innerHTML = `
                <span>${user.fullName}</span>
                <span>${user.email}</span>
                <span>${user.joined}</span>
                <span class="${user.active ? "admin-active" : "admin-pending"}">
                    ${user.active ? "Active" : "Inactive"}
                </span>
            `;

            table.appendChild(row);
        });

    } catch (error) {
        console.error("Admin users error:", error);
    }
}
if (window.location.pathname.includes("admin-dashboard.html")) {
    loadAdminDashboard();
    loadAdminUsers();
}