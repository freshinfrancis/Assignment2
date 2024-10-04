## Distributed Systems Assignment 2
*Student:* Freshin Francis (a1942887)

# Content Server
- The Content Server is responsible for transmitting weather data to an Aggregation Server through a socket connection. It reads weather data from a specified feed file, converts it to JSON format, and sends it via HTTP PUT requests. The server includes retry logic for connection failures, tracks the last active time for each socket, and maintains a Lamport clock to ensure synchronization with the Aggregation Server.

# Key Features:
- Reads and converts weather data from a feed file into JSON format.
- Retries sending data in case of transmission failures to ensure reliable communication.
- Tracks the last active time for each socket and responds with appropriate HTTP status codes based on the success or failure of the data transfer.
- Maintains a Lamport clock, which is updated and included with every PUT request to ensure proper synchronization with the Aggregation Server.


# Aggregation Server
- The Aggregation Server acts as a central receiver for data from multiple Content Servers. It manages client connections, processes requests, and aggregates weather data provided by these servers. The server also maintains its own Lamport clock to synchronize incoming data and ensure correct ordering of events from multiple sources.

# Key Features:
- Centralizes the collection of weather data from various sources, organizing it for easy access.
- Handles incoming client connections concurrently, ensuring data integrity.
- Stores weather data in the directory and automatically purges outdated data (i.e., data not received in the last 30 seconds or from inactive sources).
- Processes HTTP GET and PUT requests to facilitate weather data submission and retrieval.
- Provides detailed error messages and HTTP status codes to indicate the success or failure of requests.
- Maintains an internal Lamport clock to ensure correct ordering of events and accurate timestamps for incoming and out weather data, coordinating with the clocks of the connected Content Servers.


# GET Client
- The GET Client is an application that retrieves weather data from the Aggregation Server. It sends HTTP GET requests and processes the responses from the server, formatting the data for easier readability. The client also maintains its own Lamport clock, which is updated with each request, ensuring correct event ordering and synchronization with the Aggregation Server.

# Key Features:
- Sends HTTP GET requests to retrieve the most recent weather data.
- Implements a heartbeat mechanism to keep the connection active.
- Utilizes a Lamport clock to timestamp requests, ensuring proper ordering of events and avoiding conflicts with data received from the server.
- Filters and formats the retrieved weather data for improved readability.

------------------------------------------------------------------------------------------------------------------------------------------------

# Running the Project in Eclipse
# Prerequisites:
•	Ensure you have the Java Development Kit (JDK) installed.
•	Place the JSON library (e.g., Gson) in a lib/ folder within your project directory.

# Importing the Project:
- 1.	Open Eclipse and select File > Import.
- 2.	Choose Existing Projects into Workspace and click Next.
- 3.	Browse to the location of your project and select it, then click Finish.
   
# Compiling and Running the Programs:
- Aggregation Server:
- 1.	Open the AggregationServer.java file.
- 2.	Right-click on the file in the Package Explorer and select Run As > Java Application.
- 3.	Default port of Aggregation set as 4567. This can be modified by passing different arguments in run configurations.
- 4.	Provide the port number as an argument in the run configuration:
-    o	Select Java Application for your AggregationServer.
-    o	Click Apply and then Run.

Content Server:
- 1.	Open the ContentServer.java file.
- 2.	Right-click on the file in the Package Explorer and select Run As > Java Application.
- 3.	Provide the necessary arguments in the run configuration:
-    o	Click on Run > Run Configurations...
-    o	Select Java Application for your ContentServer.
-    o	In the Arguments tab, add: http://localhost:4567.
-    o	Click Apply and then Run.

GET Client:
- 1.	Open the GETClient.java file.
- 2.	Right-click on the file in the Package Explorer and select Run As > Java Application.
- 3.	Provide the server URL as an argument:
-    o	Click on Run > Run Configurations...
-    o	Select Java Application for your GETClient.
-    o	In the Arguments tab, add: http://localhost:8080.
-    o	Click Apply and then Run.

