# a1942887 Assignment2
Distributed Systems Assignment 2
- Creating a Weather App using Maven

- Started the assignment by creating basic weather app with default input values in content server and updated that data in the aggregation server and then initiated GET request from Client. All this helped to understand how the GET and PUT Request works as well as how the client and server communicate using Sockets.

- In the next step, tried to upload data from the default json file from content server to aggregation server.

- Next, I modified the content server code to print the put message as requested. Modified the aggregation server to read both the header and json data and save in the handlePUT request function.

- Modified the Aggregation Server and created a intermediate storage temporary file so that the Server can withstand crashes. The data is stored in temp file and after validation is sent to the permanent data file.

- Next, I modified the Aggregation Server Code to include the success and failure codes while execution

- Next, I modified the Aggregation Server Code to have default port as 4567 and also have the option to put port no in command line argument

- Aggregation server has been designed to stay current and will remove any items in the JSON that have come from content servers which it has not communicated with for 30 seconds. The server takes the file in hashmap along with data and timestamp to create weather data and the data is transformed again to json format when get request is called.

- GETClient code modified to enable using custom port no while passing argument on command line. The code is also now able to strip the json format of the data sent from aggregation server display in plain txt format.
