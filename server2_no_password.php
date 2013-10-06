<?php

@ $db = mysql_pconnect("<database address>","<username>",
      "<database password>"
);
if(!$db) {
   echo "e";
   return;
}

mysql_select_db('<database name>');

switch( $_GET[ 'action' ] )
{
   case 'connect': connect();
      break;
   case 'roomnames': roomNames();
      break;
   case 'gamenames': gameNames();
      break;
   case 'serveraddr': serverAddr();
      break;
   case 'join': joinGame();
      break;
   case 'create': create();
      break;
   case 'unjoin': unjoin();
      break;
   case 'requeststart': requestStart();
      break;
   case 'acceptrequest': acceptRequest();
      break;
   case 'info': updateStatus();
      break;
   default: echo "e";
}

function connect()
{
   $playerId = -1;//not set yet

   $name = $_GET['playername'];
   if(!$name) {
      echo 'i';
      return;
   }
   $name = addslashes(html_entity_decode($name));
   $find = array("\t","\n"," ","\r","\f", "\x0B");
   $name = str_replace($find, "_", $name);
   if(strlen($name) > 20 || strlen($name) < 1) {
      echo 'i';
      return;
   }
   $password = 0 + $_GET['password'];
   $ip = $_SERVER['REMOTE_ADDR'];
   $serverPort = 0 + $_GET['serverport'];
   if( $serverPort < 0 || $serverPort > 65536 ) {
      echo "e";
      return;
   }

   $rowPassword = 0;
      
   $oldentry = mysql_query( "SELECT password,ip,connectedTime,playerId,gameId FROM players
         WHERE playerName='$name'"
   );
   if(mysql_num_rows($oldentry) != 0) {
      $row = mysql_fetch_array($oldentry);
      $rowPassword = 0 + $row['password'];
      $connectedTime = $row['connectedTime'];
      $rowIp = $row['ip'];
      $playerId = 0 + $row['playerId'];
      if( (time() - $connectedTime >= (30*24*60*60)) || (($rowIp==$ip) && ($password==$rowPassword))) {
         //first, unjoin the old game, if it exists
         $query = "SELECT gameId FROM games WHERE gameId=" . intval($row['gameId']);
         $oldGameResult = mysql_query($query);
         if($oldGameResult) {
            if(mysql_fetch_array($oldGameResult)) {
               //unjoin the old game
               if(!unjoin_basic($playerId,intval($row['gameId']))) {
                  echo "e";
                  return;
               }
               //else continue as usual
            }
            //else continue as usual
         }
         //else continue as usual

         $query = "UPDATE players
               SET wasRequestRejected=FALSE,votedAlready=FALSE,gameId=NULL,
               ip='$ip',serverPort=$serverPort,connectedTime=" . time() .
               " WHERE playerId=$playerId";
         if(!mysql_query($query)) {
            echo "e";
            return;
         }
      }
      else {
         echo 'n';//name already taken
         return;
      }
   }
   else {
      $rowPassword = rand(-2000000000,2000000000);
      $query = "INSERT INTO players (playerName,password,ip,serverPort,connectedTime)
            VALUES ('$name','$rowPassword','$ip',$serverPort," . time() . ")";
      if(!mysql_query($query)) {
         echo "e";
         return;
      }
      //get the playerId
      $query = "SELECT playerId FROM players WHERE playerName='$name'";
      $result = mysql_query($query);
      if(!$result) {
         echo "e";
         return;
      }
      $row = mysql_fetch_array($result);
      if(!$row) {
         echo "e";
         return;
      }
      $playerId = 0 + $row['playerId'];
   }

   //getting to here indicates success
   echo 's';
   echo "\n$playerId";
   echo "\n$rowPassword";
}

function roomNames()
{
   $result = mysql_query( "SELECT roomName FROM rooms" );
   if(!$result) {
      echo "e";
      return;
   }
   //else
   echo 's';//success
   while($row = mysql_fetch_array($result)) {
      echo "\n" . htmlspecialchars($row['roomName']);
   }
}

function gameNames()
{
   $roomName = addslashes(html_entity_decode($_GET['room']));
   $query = "SELECT gameName, description, gameId, serverIp, serverPort,
         difficulty, requested, totalPlayers, isStarting, startTime
         FROM games WHERE roomId=(SELECT roomId FROM rooms WHERE roomName='$roomName')";
   $result = mysql_query($query);
   if(!$result) {
      echo "e";
      return;
   }

   echo "s";//success

   while($row = mysql_fetch_array($result)) {
      $gameId = $row['gameId'];
      if($row['isStarting']) {
         //if the 20 is changed below, also change it in unjoin_basic
         if(time() - $row['startTime'] >= 20) {//delete the game
            $query = "DELETE FROM games WHERE gameId=$gameId";
            mysql_query($query);
            //ignore errors here; they are unimportant to overall role of this function
         }
         continue;//either way, skip this entry; it cannot be joined
      }
      $line = $row['gameName'] . "\t" . $row['description'] . "\t" . $row['gameId'] . "\t" .
            $row['serverIp'] . "\t" . $row['serverPort'] . "\t" .
            "\t" . $row['difficulty'] . "\t" . $row['requested'] . "\t" . $row['totalPlayers'];
      $players = mysql_query("SELECT playerName FROM players WHERE gameId=$gameId");
      $playerCount = 0;
      while($playerRow = mysql_fetch_array($players)) {
         $line = $line . "\t" . $playerRow['playerName'];
         $playerCount++;
      }
      echo "\n" . htmlspecialchars($line);
   }
}

function serverAddr()
{
   $playerId = 0 + $_GET['playerid'];
   $result = mysql_query( "SELECT ip, gameId FROM players WHERE playerId=$playerId" );
   if(!$result) {
      echo "e1";
      return;
   }
   $ip = $_SERVER['REMOTE_ADDR'];
   $row = mysql_fetch_array($result);
   if(!$row) {
      echo "e2";
      return;
   }
   if($row['ip'] != $ip) {
      echo "e3";
      return;
   }
   if(!intval($row['gameId'])) {
      $gameId = 0 + $_GET['gameid'];
   }
   else $gameId = 0 + $row['gameId'];

   $result = mysql_query( "SELECT serverIp, serverPort
         FROM games WHERE gameId=$gameId" );
   if(!$result) {
      echo "e4";
      return;
   }
   $row = mysql_fetch_array($result);
   if(!$row) {
      echo "e5";
      return;
   }
   $isPlayerLAN = ($row['serverIp'] == $ip);
   $isPlayerLANChar = ($isPlayerLAN)? "l": "n";

   echo 's';//success
   echo "\n" . $row['serverIp'];
   echo "\n" . $row['serverPort'];
   echo "\n" . $isPlayerLANChar;
}

function joinGame()
{
   $playerId = 0 + $_GET['playerid'];
   $result = mysql_query( "SELECT ip, gameId FROM players WHERE playerId=$playerId" );
   if(!$result) {
      echo "e";
      return;
   }
   $ip = $_SERVER['REMOTE_ADDR'];
   $row = mysql_fetch_array($result);
   if(!$row) {
      echo "e";
      return;
   }
   if($row['ip'] != $ip) {
      echo "e";
      return;
   }
   if($row['gameId']) {
      echo "e";
      return;
   }

   $playerGamePassword = rand(-2000000000,2000000000);

   $gameId = 0 + $_GET['gameid'];

   $result = mysql_query( "SELECT isStarting FROM games
         WHERE gameId=$gameId"
   );
   if(!$result) {
      echo "e";
      return;
   }
   $row = mysql_fetch_array($result);
   if(!$row) {
      echo "e";
      return;
   }
   if($row['isStarting']) {
      echo "l";//too late
      return;
   }

   mysql_query("START TRANSACTION");

   $query = "UPDATE players
         SET wasRequestRejected=FALSE,votedAlready=FALSE,
         gamePassword=$playerGamePassword,gameId=$gameId
         WHERE playerId=$playerId";
   mysql_query($query);
   $query = "UPDATE games
         SET totalPlayers=totalPlayers+1
         WHERE gameId=$gameId";
   mysql_query($query);
   $query = "UPDATE games
         SET isStarting=TRUE, startTime=" . time() .
         " WHERE gameId=$gameId
         AND totalPlayers >= requested";
   mysql_query($query);
   if(mysql_error()) {
      mysql_query("ROLLBACK");
      echo "e";
      return;
   }

   //success
   mysql_query("COMMIT");
   echo "s";
   return;
}

function create()
{
   $playerId = 0 + $_GET['playerid'];
   $result = mysql_query( "SELECT ip, playerName, serverPort, gameId FROM players
          WHERE playerId=$playerId"
   );
   if(!$result) {
      echo "n";
      return;
   }
   $ip = $_SERVER['REMOTE_ADDR'];
   $row = mysql_fetch_array($result);
   if(!$row) {
      echo "e";
      return;
   }
   if($row['ip'] != $ip) {
      echo "e";
      return;
   }
   if(intval($row['gameId'])) {
      echo "e";
      return;
   }
   $playerName = $row['playerName'];
   $serverPort = $row['serverPort'];
   $playerGamePassword = rand(-2000000000,2000000000);
   $gameName = addslashes(html_entity_decode($_GET['gamename']));
   if(strlen($gameName) > 50) {
      echo "n";
      return;
   }
   $description = addslashes(html_entity_decode($_GET['description']));
   if(strlen($description) > 200) {
      echo "d";
      return;
   }
   $difficulty = 0 + $_GET['difficulty'];
   if($difficulty < 1 || $difficulty > 5) {
      echo "l";
      return;
   }
   $requested = 0 + $_GET['players'];
   if($requested < 2 || $requested > 10) {
      echo "p";
      return;
   }
   $roomName = addslashes(html_entity_decode($_GET['roomname']));
   //insert the row
   mysql_query("START TRANSACTION");
   $query = "INSERT INTO games (gameName,description,creatorId,creatorName,roomId,difficulty,
          requested,serverId,serverIp,serverPort,totalPlayers)
          VALUES ('$gameName','$description',$playerId,'$playerName',
          (SELECT roomId FROM rooms WHERE roomName='$roomName')
          ,$difficulty,$requested,$playerId,'$ip',$serverPort,1)";
   mysql_query($query);

   $query = "UPDATE players
         SET wasRequestRejected=FALSE,votedAlready=FALSE,
         gamePassword=$playerGamePassword,gameId=
         (SELECT gameId FROM games WHERE creatorId=$playerId LIMIT 1)
         WHERE playerId=$playerId";
   mysql_query($query);
   if(mysql_error()) {
      mysql_query("ROLLBACK");
      echo "q";
      return;
   }
   //else don't commit yet

   //get the gameId
   $query = "SELECT gameId FROM games WHERE creatorId=$playerId";
   $result = mysql_query($query);
   if(!$result) {
      mysql_query("ROLLBACK");
      echo "e";
      return;
   }
   $row = mysql_fetch_array($result);
   if(!$row) {
      mysql_query("ROLLBACK");
      echo "e";
      return;
   }
   $gameId = intval($row['gameId']);

   mysql_query("COMMIT");
   echo 's';//success

   //echo the game data, in the same format as gameNames
   $line = $gameName . "\t" . $description . "\t" . $gameId . "\t" .
            $ip . "\t" . $serverPort . "\t" .
            "\t" . $difficulty . "\t" . $requested . "\t1\t" . $playerName;
   echo "\n" . htmlspecialchars($line);
}

function unjoin()
{
   $playerId = 0 + $_GET['playerid'];
   $result = mysql_query( "SELECT ip, gameId FROM players WHERE playerId=$playerId" );
   if(!$result) {
      echo "e";
      return;
   }
   $ip = $_SERVER['REMOTE_ADDR'];
   $row = mysql_fetch_array($result);
   if(!$row) {
      echo "e";
      return;
   }
   if($row['ip'] != $ip) {
      echo "e";
      return;
   }

   $gameId = $row['gameId'];
   if(!$gameId) {
      echo "e";
      return;
   }

   if(!unjoin_basic($playerId,$gameId)) {
      echo "e";
      return;
   }
   //else
   echo "s";
}

//returns boolean success code
function unjoin_basic($playerId,$gameId)
{
   /* No matter what, try to clear the player's name, e.g., in case the game
         was deleted (and thus the rest of this function will error).
   */
   mysql_query( "UPDATE players
         SET gameId=NULL
         WHERE playerId=$playerId"
   );

   $result = mysql_query( "SELECT serverId, totalPlayers, isStarting, startTime
         FROM games WHERE gameId=$gameId"
   );
   if(!$result) {
      return false;
   }
   $row = mysql_fetch_array($result);
   if(!$row) {
      return false;
   }
   if($row['isStarting']) {
      if(time() - $row['startTime'] >= 20) {//delete the game
         $query = "DELETE FROM games WHERE gameId=$gameId";
         mysql_query($query);
         //ignore errors here; they are unimportant to overall role of this function
         return true;
      }
      else return false;
   }

   $totalPlayers = $row['totalPlayers'];
   if($row['serverId'] == $playerId) {
      //delete game
      mysql_query("START TRANSACTION");
      mysql_query( "DELETE FROM games WHERE gameId=$gameId" );
   }
   else {
      mysql_query("START TRANSACTION");
      $query = mysql_query( "UPDATE games
            SET totalPlayers=totalPlayers-1
            WHERE gameId=$gameId"
      );
   }
   //all scenarios
   if(mysql_error()) {
      mysql_query("ROLLBACK");
      return false;
   }
   //else
   mysql_query("COMMIT");
   return true;
}

function requestStart()
{
   $playerId = 0 + $_GET['playerid'];
   $result = mysql_query( "SELECT ip, gameId FROM players WHERE playerId=$playerId" );
   if(!$result) {
      echo "e";
      return;
   }
   $ip = $_SERVER['REMOTE_ADDR'];
   $row = mysql_fetch_array($result);
   if($row['ip'] != $ip) {
      echo "e";
      return;
   }

   $gameId = $row['gameId'];
   $result = mysql_query( "SELECT isStarting, requesterId, totalPlayers
         FROM games WHERE gameId=$gameId"
   );
   if(!$result) {
      echo "e";
      return;
   }
   $row = mysql_fetch_array($result);
   if(!$row) {
      echo "e";
      return;
   }
   if($row['isStarting']) {
      echo 'w';//wait
      return;
   }
   if($row['totalPlayers'] == 1) {//this is the only player
      echo 'o';
      return;
   }
   if($row['requesterId']) {//if it is not null
      echo 'a';//already request
      return;
   }

   mysql_query("START TRANSACTION");
   mysql_query( "UPDATE games
         SET requesterId=$playerId, votesFor=1
         WHERE gameId=$gameId"
   );
   mysql_query( "UPDATE players
         SET votedAlready=TRUE
         WHERE playerId=$playerId"
   );
   if(mysql_error()) {
      mysql_query("ROLLBACK");
      echo "e";
      return;
   }
   mysql_query("COMMIT");
   echo 's';//success
}

function acceptRequest()
{
   $playerId = 0 + $_GET['playerid'];
   $result = mysql_query( "SELECT ip, gameId, votedAlready FROM players
         WHERE playerId=$playerId"
   );
   if(!$result) {
      echo "e";
      return;
   }
   $ip = $_SERVER['REMOTE_ADDR'];
   $row = mysql_fetch_array($result);
   if($row['ip'] != $ip) {
      echo "e";
      return;
   }

   if($row['votedAlready']) {
      echo "e";
      return;
   }

   $gameId = $row['gameId'];
   $accept = ($_GET['value'] == "true");

   $result = mysql_query( "SELECT isStarting, requesterId, votesFor, votesAgainst,
         totalPlayers FROM games WHERE gameId=$gameId"
   );
   if(!$result) {
      echo "e";
      return;
   }
   $row = mysql_fetch_array($result);
   if(!$row) {
      echo "e";
      return;
   }
   $requesterId = $row['requesterId'];
   if($row['isStarting'] || is_null($requesterId)) {
      echo 'n';
      return;
   }

   $votesFor = $row['votesFor'];
   $votesAgainst = $row['votesAgainst'];
   $totalPlayers = $row['totalPlayers'];

   mysql_query("START TRANSACTION");
   if($accept) {
      mysql_query( "UPDATE games
            SET votesFor=votesFor+1
            WHERE gameId=$gameId"
      );
      mysql_query( "UPDATE players
            SET votedAlready=TRUE
            WHERE playerId=$playerId"
      );
      if(($votesFor+1) * 3/2 >= $totalPlayers) {
         mysql_query( "UPDATE games
               SET isStarting=TRUE, startTime=" . time() .
               " WHERE gameId=$gameId"
         );
      }
   }
   else {
      mysql_query( "UPDATE games
            SET votesAgainst=votesAgainst+1
            WHERE gameId=$gameId"
      );
      mysql_query( "UPDATE players
            SET votedAlready=TRUE
            WHERE playerId=$playerId"
      );
      if(($votesAgainst+1) * 3 >= $totalPlayers) {
         mysql_query( "UPDATE players
               SET wasRequestRejected=TRUE
               WHERE playerId=$requesterId"
         );
         mysql_query( "UPDATE players
               SET votedAlready=FALSE
               WHERE gameId=$gameId"
         );
         mysql_query( "UPDATE games
               SET requesterId=NULL, votesFor=0, votesAgainst=0
               WHERE gameId=$gameId"
         );
      }
   }

   if(mysql_error()) {
      mysql_query("ROLLBACK");
      echo 'e';
      return;
   }

   mysql_query("COMMIT");
   echo 's';//success
}

function updateStatus()
{
   $playerId = 0 + $_GET['playerid'];
   $result = mysql_query( "SELECT ip, gameId, votedAlready, wasRequestRejected
         FROM players WHERE playerId=$playerId"
   );
   if(!$result) {
      echo "e";
      return;
   }
   $ip = $_SERVER['REMOTE_ADDR'];
   $playerRow = mysql_fetch_array($result);
   if($playerRow['ip'] != $ip) {
      echo "e";
      return;
   }

   if(!intval($playerRow['gameId'])) {//no game is selected
      echo "e";
      return;
   }

   $gameId = 0 + $playerRow['gameId'];

   $result = mysql_query( "SELECT isStarting, serverKnows, serverId, requesterId, totalPlayers,
         serverIp, serverPort FROM games WHERE gameId=$gameId"
   );
   if(!$result) {
      echo "e";
      return;
   }
   if(mysql_num_rows($result) == 0) {
      //the server left, disbanding the game
      echo "d";
      return;
   }

   $gameRow = mysql_fetch_array($result);


   $requesterId = $gameRow['requesterId'];

   //get the data
   $isServerBool = ($gameRow['serverId'] == $playerId);
   $isServer = ($isServerBool)? "t": "f";
   $isStartingBool = false;
   if($isServerBool || $gameRow['serverKnows']) {
      $isStartingBool = $gameRow['isStarting'];
   }
   $isGameStarting = ($isStartingBool)? "t": "f";
   $isStartRequestPending = ( (!is_null($requesterId)) &&
         (!$playerRow['votedAlready']) )? "t": "f";
   $requestSender = "";
   if($requesterId) {//if requesterId is not null
      $result = mysql_query( "SELECT playerName FROM players WHERE playerId=$requesterId" );
      if(!$result) {
         echo "e";
         return;
      }
      $row = mysql_fetch_array($result);
      if(!$row) {
         echo "e";
         return;
      }
      $requestSender = $row['playerName'];
   }

   $wasRequestRejected = ($playerRow['wasRequestRejected'])? "t": "f";
   if($playerRow['wasRequestRejected']) {//reset wasRequestRejected
      $query = "UPDATE players SET wasRequestRejected=FALSE WHERE playerId=$playerId";
      mysql_query($query);//ignore errors
   }
   $playersSoFar = $gameRow['totalPlayers'];
   $serverIp = $gameRow['serverIp'];
   $serverPort = $gameRow['serverPort'];
   $playerNameResult = null;
   $playerIpResult = null;
   if($isStartingBool) {
      //get the players' names
      $playerDataResult = mysql_query( "SELECT playerId, playerName, gamePassword
            FROM players WHERE gameId=$gameId ORDER BY playerId"
      );
      if(!$playerDataResult) {
         echo "e";
         return;
      }
   }

   //echo the data
   echo "s";//first tell client that the db queries were successful
   echo "\n" . $isGameStarting;
   echo "\n" . $isServer;
   echo "\n" . $isStartRequestPending;
   echo "\n" . $wasRequestRejected;
   echo "\n" . $playersSoFar;
   echo "\n" . htmlspecialchars($requestSender);

   if($isStartingBool) {
      echo "\n" . $serverIp;
      echo "\n" . $serverPort;
      echo "\n" . $ip;//this player's ip address
      //echo the players' names and id's
      $playerIdInGame = 0;
      $thisPlayerGamePassword = 0;
      $i = 0;
      while($row=mysql_fetch_array($playerDataResult)) {
         echo "\n" . htmlspecialchars($row['playerName']);
         if( $row['playerId'] == $playerId ) {//found this player
            //the order of the names will be the same for server and client
            //so the id's generated by the iteration count are constant between players
            $playerIdInGame = $i;
            $thisPlayerGamePassword = $row['gamePassword'];
         }
         $i++;
      }
      echo "\n" . $playerIdInGame;
      if($isServerBool) {//echo the players' gamePasswords
         mysql_data_seek($playerDataResult, 0);//rewind
         while($row=mysql_fetch_array($playerDataResult)) {
            echo "\n" . $row['gamePassword'];
         }

         //update serverKnows
         $query = "UPDATE games SET serverKnows=TRUE WHERE gameId=$gameId";
         mysql_query($query);//nothing to do about errors
      }
      //echo this player's gamePassword
      echo "\n" . $thisPlayerGamePassword;
   }
}
?>
