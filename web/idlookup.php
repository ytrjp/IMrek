<?php

require 'config.php';

switch(intval($_GET['action']) {
	case 0:			// Lookup
		if (!isset($_GET['id'])) {
			echo "user";
			exit;
		}

		$db = new PDO("mysql:host=".$DB_HOST.";dbname=".$DB_NAME,$DB_USER,$DB_PASS);

		$sth = $db->prepare("SELECT username FROM users WHERE device_id = ?");

		$res = $sth->execute(array($_GET['id']));

		echo $res->fetch(PDO::FETCH_ASSOC)["username"];	
		exit;
		break;
	case 1:			// register
		if (!isset($_GET['id']) || !isset($_GET['username']) || !isset($_GET['password'])) {
			echo "invalid";
			exit;
		}
		require 'BCrypt.class.php';

		$db = new PDO("mysql:host=".$DB_HOST.";dbname=".$DB_NAME,$DB_USER,$DB_PASS);

		$bcrypt = new Bcrypt();
		$password = $bcrypt->hash($_GET['password']);

		$sth = $db->prepare("INSERT INTO users VALUES(?, ?, ?)");

		$res =  $sth->execute(array($_GET['id'], $_GET['username'], $password));

		if (!$res) {
			echo "Failed";
		} else {
			echo "Success!";
		}
		exit;
		break;
	case 2:
		if (!isset($_GET['username']) || !isset($_GET['password'])) {
			echo "invalid";
			exit;
		}

		$db = new PDO("mysql:host=".$DB_HOST.";dbname=".$DB_NAME,$DB_USER,$DB_PASS);

		$bcrypt = new Bcrypt();
		
		$db = new PDO("mysql:host=".$DB_HOST.";dbname=".$DB_NAME,$DB_USER,$DB_PASS);

		$sth = $db->prepare("SELECT password FROM users WHERE device_id = ? AND username = ?");

		$res = $sth->execute(array($_GET['id'], $_GET['username']));
		if ($res->rowCount() != 1) {
			echo "invalid";
			exit;
		}
		if (!$bcrypt->verify($_GET['password'], $res->fetch(PDO::FETCH_ASSOC)["password"])) {
			echo "Invalid password";
			exit;
		}

		echo "Success!";
		exit;
		break;
	case default:
		echo "Unknown request";
		break;
}


?>