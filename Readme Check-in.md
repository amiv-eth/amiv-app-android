# amiv Check-in Microapp
**[Check-in Serverside/Website](https://gitlab.ethz.ch/amiv/amiv-checkin)**  
**[Check-in iOS](https://gitlab.ethz.ch/amiv/amiv-checkin-app-ios)**

## Summary 
Android app for scanning legi barcodes and sending to the AMIV checkin server, see [project here](https://gitlab.ethz.ch/amiv/amiv-checkin). 
The idea is to have one or more helpers checking in people for an event by scanning their legis, ensuring only permitted people can enter.
Used for events, freebies, PVK and GV to see if people are registered for the events or to check people in/out of the GV.

## Structure
The app has been developed for simplicity and ease of use, everything complex is on the website/server project [here](https://gitlab.ethz.ch/amiv/amiv-checkin). 
The app mainly scans and sends legi numbers to the server and then shows the response.

The app consist of the following activities (screens):
* **Main** - Login screen for entering an event pin
* **Scanning** - Barcode scanning or manual input used to then make a request to the server, the response is then nicely displayed
* **Member List** - Used to display the data: stats, event info and members. Similar to the checkin website.
   * This also has a *search activity*, to search through the members list
* **Settings** - Only for manually setting the server address and other settings. Uses `core/Settings` for storing values

Also note the static *ServerRequests*, which is used to handle most (not all) requests for data from the server.
The *EventDatabase* is also central, use the static instance to access it.

### Event Types
1. **Event** - has a list of permitted users, which can be checked in or out
    1. **PVK** - *not fully implemented/tested*
    2. **GV** - Everyone is a member of the event. Want to see membership
2. **Freebie** - everyone is a member, each person has a counter, with a limit set on the website. e.g. every person gets *two* free beers