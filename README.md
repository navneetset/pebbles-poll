# pebbles-poll
Server-side fabric mod. Addition to the Pebble's Minecraft mod series. Generate a clickable live poll with scoreboard update.

Supported versions:
Fabric 1.19.2

Ensure that you have Fabric Language Kotlin (atleast 1.8.20) installed.
Mod page:
https://www.curseforge.com/minecraft/mc-mods/pebbles-polling-vote-system 

 
## Usage
Running a Minecraft server, I often find myself having to go to Discord to create polls that is sometimes only relevant to people who are online in-game now. So I have decided to create this polling system with live update. You can have 1 live poll at a time. Currently you need to be an operator to use the poll.

 

/pebblespoll create <question>

/pebblespoll <time> (in minutes)

/pebblespoll options option1, option2, option3, .... (comma-separated)

 

After creating options, the poll will start. Everyone can either click the choice in chat or type:

/pebblespoll vote <option_number>

 

 To forcefully bring a poll to an end:

/pebblespoll end

 

To clear the scoreboard

/pebblespoll clear
