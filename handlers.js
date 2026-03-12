/**
 * BUTTON CLICK HANDLERS
 *
 * Each button on the homepage has its own handler function below.
 * Replace the placeholder console.log with your own implementation.
 */

 async function onButton1Click() {
  console.log("Button 1 clicked: Fetching Cat Fact...");
  
  try {
    const response = await fetch('https://catfact.ninja/fact');
    const data = await response.json();

    document.body.innerHTML = `
      <div style="padding: 40px; font-family: sans-serif; line-height: 1.6;">
        <h1>🐱 Random Cat Fact</h1>
        <hr>
        <p style="font-size: 1.5rem; color: #333;">"${data.fact}"</p>
        
        <h3>About this Data:</h3>
        <p>This page displays a random fact about cats retrieved from the <strong>CatFact Ninja Public API</strong>. 
        The data represents a single string ('fact') and its character length ('length').</p>
        
        <button onclick="window.location.reload()">Go Back</button>
      </div>
    `;
  } catch (error) {
    console.error("Error fetching API:", error);
    alert("Failed to load API data.");
  }
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