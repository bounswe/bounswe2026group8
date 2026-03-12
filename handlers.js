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
  window.location.href = "button3.html";
}

function onButton4Click() {
  const newWindow = window.open("", "_blank");

  fetch("https://uselessfacts.jsph.pl/api/v2/facts/random?language=en")
    .then(response => response.json())
    .then(data => {
      newWindow.document.write(`
        <html>
          <head><title>Random Fun Fact</title></head>
          <body style="text-align:center; font-family:sans-serif; padding:20px;">
            <h1>Random Fun Fact</h1>
            <p style="font-size:20px; max-width:600px; margin:20px auto;">${data.text}</p>
            <p><a href="${data.source_url}" target="_blank">Source</a></p>
            <p style="color:gray; font-size:14px;">
              This data comes from the Useless Facts API (uselessfacts.jsph.pl).<br>
              It returns a random interesting fact on each request,
              along with a source link for verification.
            </p>
          </body>
        </html>
      `);
    })
    .catch(error => {
      newWindow.document.write(`
        <html>
          <body style="text-align:center; font-family:sans-serif; padding:20px;">
            <h1>Error</h1>
            <p>Could not fetch data: ${error.message}</p>
          </body>
        </html>
      `);
    });
}

function onButton5Click() {
  window.open("button5.html", "_blank");
}

function onButton6Click() {
  window.location.href = "pokemon.html";
}

function onButton7Click() {
  fetch("https://jsonplaceholder.typicode.com/todos/1")
    .then(response => response.json())
    .then(data => {

      var newWindow = window.open("", "_blank");

      newWindow.document.write("<h1>Public API Result</h1>");
      newWindow.document.write("<pre>" + JSON.stringify(data, null, 2) + "</pre>");

    })
    .catch(err => console.log(err));
}
