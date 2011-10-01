<?php

require 'config.php';
require 'util.php';

//Always return JSON in the form of:
// {'error': 1, 'message': 'Either error or success message', 'data': {...data...}}
// Data should be omitted if no data needs to be returned

switch(intval($_POST['action']) {
	//Lookups can also be done via the broker, if we have PHP connected as a client.
	// case 0:			// Lookup
	// 	if (!isset($_POST['id'])) {
	// 		echo "user";
	// 		exit;
	// 	}

	// 	$db = new PDO("mysql:host=".$DB_HOST.";dbname=".$DB_NAME,$DB_USER,$DB_PASS);

	// 	$sth = $db->prepare("SELECT username FROM users WHERE device_id = ?");

	// 	$res = $sth->execute(array($_POST['id']));

	// 	echo $res->fetch(PDO::FETCH_ASSOC)["username"];	
	// 	exit;
	// 	break;
	case 0: //Register
		if (!isset($_POST['id']) || !isset($_POST['username']) || !isset($_POST['password'])) {
			echo "invalid";
			exit;
		}
		require 'BCrypt.class.php';

		$db = new PDO("mysql:host=".$DB_HOST.";dbname=".$DB_NAME,$DB_USER,$DB_PASS);

		$bcrypt = new Bcrypt();
		$password = $bcrypt->hash($_POST['pass']);

		$sth = $db->prepare("INSERT INTO users VALUES(?, ?, ?)");

		$res =  $sth->execute(array($_POST['id'], $_POST['user'], $password));

		if (!$res) {
			echo "{'error': 1, 'message':'Database Error'}";
			exit;
		} else {
			addMqttUser($_POST['user'], $password);
			sendReloadSignal();
			echo "{'error': 0, 'message':'Successfully Reistered'}";
			exit;
		}
		break;
	//Logins are handled by the MQTT broker - we just need to update the password file when someone registers/deregisters
	//This case handles checking to see if a user/password combo exists
	case 1:
		if (!isset($_POST['username']) || !isset($_POST['password'])) {
			echo "{'error': 1, 'message':'An internal error occured'}";
			exit;
		}

		$db = new PDO("mysql:host=".$DB_HOST.";dbname=".$DB_NAME,$DB_USER,$DB_PASS);

		$bcrypt = new Bcrypt();
		
		$db = new PDO("mysql:host=".$DB_HOST.";dbname=".$DB_NAME,$DB_USER,$DB_PASS);

		$sth = $db->prepare("SELECT password FROM users WHERE device_id = ? AND username = ?");

		$res = $sth->execute(array($_POST['id'], $_POST['username']));
		if ($res->rowCount() != 1) {
			echo "{'error': 1, 'message':'Username does not exist'}";
			exit;
		}
		if (!$bcrypt->verify($_POST['password'], $res->fetch(PDO::FETCH_ASSOC)["password"])) {
			echo "{'error': 1, 'message':'Invalid password'}";
			exit;
		}

		echo "{'error': 0, 'message':'User exists'}";
		exit;
		break;
	case 2: //Change Password or Username
		exit;
		break;
	case 3: //Deregister
		exit;
		break;
	case default:
		echo "Unknown request";
		break;
}

?>