# Distributed Systems Assignment 2
Student :: a1942887

## Content Server
- The Content Server is responsible for transmitting weather data to an Aggregation Server through a socket connection. It reads weather data from a specified feed file, converts it to JSON format, and sends it via HTTP PUT requests. The server includes retry logic for connection failures, tracks the last active time for each socket, and maintains a Lamport clock to ensure synchronization with the Aggregation Server.

## Key Features:
- Reads and converts weather data from a feed file into JSON format.
- Retries sending data in case of transmission failures to ensure reliable communication.
- Tracks the last active time for each socket and responds with appropriate HTTP status codes based on the success or failure of the data transfer.
- Maintains a Lamport clock, which is updated and included with every PUT request to ensure proper synchronization with the Aggregation Server.


## Aggregation Server
- The Aggregation Server acts as a central receiver for data from multiple Content Servers. It manages client connections, processes requests, and aggregates weather data provided by these servers. The server also maintains its own Lamport clock to synchronize incoming data and ensure correct ordering of events from multiple sources.

### Key Features:
- Centralizes the collection of weather data from various sources, organizing it for easy access.
- Handles incoming client connections concurrently, ensuring data integrity.
- Stores weather data in the directory and automatically purges outdated data (i.e., data not received in the last 30 seconds or from inactive sources).
- Processes HTTP GET and PUT requests to facilitate weather data submission and retrieval.
- Provides detailed error messages and HTTP status codes to indicate the success or failure of requests.
- Maintains an internal Lamport clock to ensure correct ordering of events and accurate timestamps for incoming and out weather data, coordinating with the clocks of the connected Content Servers.


## GET Client
- The GET Client is an application that retrieves weather data from the Aggregation Server. It sends HTTP GET requests and processes the responses from the server, formatting the data for easier readability. The client also maintains its own Lamport clock, which is updated with each request, ensuring correct event ordering and synchronization with the Aggregation Server.

### Key Features:
- Sends HTTP GET requests to retrieve the most recent weather data.
- Implements a heartbeat mechanism to keep the connection active.
- Utilizes a Lamport clock to timestamp requests, ensuring proper ordering of events and avoiding conflicts with data received from the server.
- Filters and formats the retrieved weather data for improved readability.

------------------------------------------------------------------------------------------------------------------------------------------------
# Running the Project using Command Line

Directory Structure :: 
src/main/com/weather/app/

## Building the Project
1.	Navigate to the root directory of the project.
2.	Compile the Java files:

`javac -d bin src/main/com/weather/app/*.java`

## Running the Servers and Clients
1. Start the Aggregation Server
- Open a terminal and run the following command to start the Aggregation Server:

`java -cp bin com.weather.app.AggregationServer <server-address:port>`

Usage:
`java -cp bin com.weather.app.AggregationServer`

2. Start the Content Server
- In a new terminal, run the following command to start the Content Server. Replace <server-address:port> with the address and port of the Aggregation Server, and provide the path to the weather data file.

`java -cp bin com.weather.app.ContentServer <server-address:port> <file-path>`

Usage:
`java -cp bin com.weather.app.ContentServer localhost:4567 txt.txt`

3. Run the GET Client
- In another terminal, run the GET Client with the server address and port. Optionally, you can specify a station ID.

`java -cp bin com.weather.app.GETClient <server-address:port> [station-id]`

Usage:
`java -cp bin com.weather.app.GETClient localhost:4567`

------------------------------------------------------------------------------------------------------------------------------------------------
# Running the Project in Eclipse
## Prerequisites
Before running the project, ensure that the following requirements are met:
-•	Java Development Kit (JDK) installed on your machine.
-•	JSON library (e.g., Gson) placed in a lib/ folder within your project directory.

## Importing the Project in Eclipse
To import the project into Eclipse:
1.	Open Eclipse.
2.	Select File > Import.
3.	Choose Existing Projects into Workspace and click Next.
4.	Browse to the location of your project directory and select it.
5.	Click Finish to import the project into the workspace.
Compiling and Running the Programs

## Aggregation Server
1. Open the AggregationServer.java file in Eclipse.
2. Right-click on the file in the Package Explorer.
3. Select Run As > Java Application.
4. The default port for the Aggregation Server is set to 4567. If needed, you can modify this by passing a different port number in the Run Configurations:
-   Click on Run > Run Configurations....
-   Select Java Application for your AggregationServer.
-   If required, in the Arguments tab, provide the port number (e.g., 4567).
-   Click Apply and then Run.

## Content Server
1. Open the ContentServer.java file in Eclipse.
2. Right-click on the file in the Package Explorer.
3. Select Run As > Java Application.
4. Provide the necessary arguments in the Run Configurations:
-   Click on Run > Run Configurations....
-   Select Java Application for your ContentServer.
-   In the the Arguments tab, add: http://localhost:4567 txt.txt.
-   Click Apply and then Run.

## GET Client
1.	Open the GETClient.java file in Eclipse.
2.	Right-click on the file in the Package Explorer.
3.	Select Run As > Java Application.
4.	Provide the server URL in the Run Configurations:
-   Click on Run > Run Configurations....
-   Select Java Application for your GETClient.
-   In the the Arguments tab, add: http://localhost:4567.
-   Click Apply and then Run.

## Running Test Cases in Eclipse
- Right click on the test classes(AggregationServerTest.java, ContentServerTest.java, GETClientTest.java and LamportClock.java) in the Package Explorer.
- Select Run as JUnit Test or run 'mvn test' in command line.
- The JUnit view will show the results of the tests.
  
## Notes
•	Ensure that the Aggregation Server is running before starting the Content Server or GET Client.

------------------------------------------------------------------------------------------------------------------------------------------------------------

# Step by Step Development Process

1. Designing the Architecture of the system:
- Drew a design sketch of the architecture of the system on draw.io.
   
2.	Initial Setup:
-	Created a basic weather app with default input values in the content server.
-	Updated the data in the aggregation server and initiated GET requests from the client.
-	This step helped in understanding how GET and PUT requests work and how the client-server communication occurs using sockets.

3.	Data Upload:
-	Attempted to upload data from the default JSON file in the content server to the aggregation server.

4.	Code Modifications:
-	Modified the content server code to print the PUT message as requested.
-	Updated the aggregation server to read both the header and JSON data, saving it in the handlePUT request function.

5.	Intermediate Storage Implementation:
-	Modified the aggregation server to create an intermediate storage temporary file, allowing the server to withstand crashes.
-	Data is now stored in a temporary file, validated, and then sent to the permanent data file.

6.	Response Codes:
-	Enhanced the aggregation server code to include success and failure codes during execution.

7.	Port Configuration:
-	Set a default port (4567) for the aggregation server.
-	Implemented the option to specify a custom port number via command-line arguments.

8.	Data Management:
-	Designed the aggregation server to automatically remove any items in the JSON that have not communicated with the server for 30 seconds.
-	Utilized a HashMap to store the file along with data and timestamps to create weather data, which is transformed back to JSON format when a GET request is made.

9.	GET Client Enhancements:
-	Modified the GET client code to allow using a custom port number via command-line arguments.
-	The client can now strip the JSON formatting of the data received from the aggregation server and display it in plain text format.

10.	Timestamping and Ordering:
-	Integrated a Lamport clock to timestamp requests, ensuring correct ordering.
-	Enhanced the processing and formatting of server responses for improved readability.

11.	Testing:
-	Included JUnit test cases for all code components to ensure functionality and reliability.

12.	Code Documentation:
-	Provided detailed comments throughout the code to enhance understanding and maintainability.

-----------------------------------------------------------------------------------------------------------------------------------------------------------------
# References

- ChatGPT for understanding how GET and PUT requests work, converting text to json, how to run code on Maven Eclipse, understanding lamport clocks, general guidance on implementing and applying code solutions, refining the comments in the code.

- Lamport, Leslie. "Time, Clocks, and the Ordering of Events in a Distributed System." Massachusetts Computer Associates, Inc.

- GeeksforGeeks: Various functionalities were implemented based on tutorials and articles found on GeeksforGeeks.
  
- Stack Overflow: Solutions and code snippets to errors encountered during implementations found from discussions and examples available on Stack Overflow.

- Github : References were taken from certain Github repositories to help in getting ideas on how to implement the solutions.

