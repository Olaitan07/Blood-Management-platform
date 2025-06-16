const express = require('express');

// Initialize Express application
const app = express();

// Middleware to parse JSON request bodies
app.use(express.json());

// Define route handler for GET requests to '/'
app.get('/', (req, res) => {
    res.send('Hello World\n');
});
//testinggit
//jnfnfnadd

// Start the server on port 5000
app.listen(5000, () => {
    console.log('Server listening at http://localhost:5000');
});