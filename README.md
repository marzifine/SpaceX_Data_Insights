# SpaceX Data Insights
## Overview
This application is a tool to get real-time information about upcoming and
past events as well as facts about SpaceX.
Application can provide the following data:
* date and name of event
* links to wikipedia page and webcast
* rocket, crew and launchpad info
## Telegram Bot view
![application-bot](src/resources/markdown/bot-app_compressed.gif)
## Command Line Interface view
![application-cli](src/resources/markdown/cli-app_compressed.gif)
## Run and build
Clone repository and run SpaceX_Data_Insights-1.0-SNAPSHOT.jar to get an app with command line interface. \
Example:
java -jar SpaceX_Data_Insights-1.0-SNAPSHOT.jar \
Arguments:
* -t to start a telegram bot \
Example:
java -jar SpaceX_Data_Insights-1.0-SNAPSHOT.jar -t \
  The program will ask for an API key (will be provided explicitly for test version of bot under the username <a href="https://t.me/SpaceXDataInsightsTest_bot">@SpaceXDataInsightsTest_bot</a>, which is identical to the main one)
  
P.S. The test bot isn't running on remote server, so in order to test it you need to actually run it locally. \
You can find and use the main bot in telegram under the username <a href="https://t.me/SpaceX_Data_Insights_Bot">@SpaceX_Data_Insights_Bot</a> (if it is in downtime, please let the developer know). 