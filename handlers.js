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
  console.log("Button 2 clicked -- implement me!");
}

function onButton3Click() {
  console.log("Button 3 clicked -- implement me!");
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
  console.log("Button 5 clicked -- implement me!");
}
