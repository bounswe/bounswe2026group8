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
  console.log("Button 2 clicked -- implement me!");
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