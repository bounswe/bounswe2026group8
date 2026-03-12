/**
 * BUTTON CLICK HANDLERS
 *
 * Each button on the homepage has its own handler function below.
 * Replace the placeholder console.log with your own implementation.
 */

function onButton1Click() {
  console.log("Button 1 clicked -- implement me!");
}

function onButton2Click() {
  const apiUrl = "https://randomuser.me/api/";

  fetch(apiUrl)
    .then(response => response.json())
    .then(data => {
      const user = data.results[0];

      const newWindow = window.open("", "_blank");

      newWindow.document.write(`
        <html>
        <head>
          <title>API Result</title>
        </head>
        <body>
          <h1>Random User API Response</h1>

          <p>
          This page displays data retrieved from the Random User API. 
          The API generates realistic but fictional user profiles that can be used 
          for testing applications or demonstrating user interfaces.
          </p>

          <h2>User Information</h2>
          <p><strong>Name:</strong> ${user.name.first} ${user.name.last}</p>
          <p><strong>Email:</strong> ${user.email}</p>
          <p><strong>Country:</strong> ${user.location.country}</p>
          <img src="${user.picture.large}" alt="User picture">
        </body>
        </html>
      `);
    })
    .catch(error => {
      console.error("Error fetching API:", error);
    });
}

function onButton3Click() {
  console.log("Button 3 clicked -- implement me!");
}

function onButton4Click() {
  console.log("Button 4 clicked -- implement me!");
}

function onButton5Click() {
  window.open("button5.html", "_blank");
}

function onButton6Click() {
  console.log("Button 6 clicked -- implement me!");
}

function onButton7Click() {
  console.log("Button 7 clicked -- implement me!");
}

function onButton8Click() {
  console.log("Button 8 clicked -- implement me!");
}