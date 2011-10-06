<?php

require 'config.php';
require 'util.php';
require 'BCrypt.class.php';

//Always return JSON in the form of:
// {'error': 1, 'message': 'Either error or success message', 'data': {...data...}}
// Data should be omitted if no data needs to be returned

switch(intval($_POST['action'])) {
	case 0: //Register
		if(!isset($_POST['username']) || !isset($_POST['password'])) {
			echo json_encode(array("error"=>1, "message"=>"Invalid parameters"));
			exit;
		}
		// Make sure the username exists and is at least 6 characters
		if (strlen($_POST['username']) < 5) {
			echo json_encode(array("error"=>1, "message"=>"Invalid username. Must be at least 6 characters"));
			exit;
		} 
		// Make sure the password exists and is at least 6 characters
		if (strlen($_POST['password']) < 6) {
			echo json_encode(array("error"=>1, "message"=>"Invalid password. Must be at least 6 characters"));
			exit; 
		} 
		
		try {
			// Make sure username doesn't exist
			$db = new PDO("mysql:host=".$DB_HOST.";dbname=".$DB_NAME,$DB_USER,$DB_PASS);

			$sth = $db->prepare("SELECT username FROM users WHERE username = ?");

			$ret = $sth->execute(array($_POST['username']));
			if (!$ret) {
				$err = $db->errorInfo();
				//echo "check";
				//print_r($err);
				echo json_encode(array("error"=>1, "message"=>$err[2]));	// 2 Is the error message in the returned array
				exit;
			}

			if ($sth->rowCount() > 0) {
				echo json_encode(array("error"=>1, "message"=>"Username already exists"));
				exit;
			}

			$bcrypt = new Bcrypt();
			$password = $bcrypt->hash($_POST['password']);

			
			$sth = $db->prepare("INSERT INTO users VALUES(?, ?)");

			$ret =  $sth->execute(array($_POST['username'], $password));

			if (!$ret) {
				$err = $db->errorInfo();
				//echo "insert";
				//print_r($err);
				echo json_encode(array("error"=>1, "message"=>$err[2]));	// 2 Is the error message in the returned array
				exit;
			} else {
				addMqttUser($_POST['username'], $_POST['password']);
				sendReloadSignal();
				echo json_encode(array("error"=>0, "message"=>"Successfully Registered"));
				exit;
			}
		} catch (PDOException $e){
			echo json_encode(array("error"=>1, "message"=>$e->getMessage()));
		}
		break;
	//Logins are handled by the MQTT broker - we just need to update the password file when someone registers/deregisters
	//This case handles checking to see if a user/password combo exists
	case 1:
		if (!isset($_POST['username']) || !isset($_POST['password'])) {
			//echo "{'error': 1, 'message':'An internal error occured'}";
			echo json_encode(array("error"=>1, "message"=>"An internal error occured"));
			exit;
		}

		// Make sure the username exists and is at least 6 characters
		if (strlen($_POST['username']) < 5) {
			echo json_encode(array("error"=>1, "message"=>"Invalid username. Must be at least 6 characters"));
			exit;
		} 
		// Make sure the password exists and is at least 6 characters
		if (strlen($_POST['password']) < 6) {
			echo json_encode(array("error"=>1, "message"=>"Invalid password. Must be at least 6 characters"));
			exit; 
		} 
				
		try {
			$db = new PDO("mysql:host=".$DB_HOST.";dbname=".$DB_NAME,$DB_USER,$DB_PASS);

			$bcrypt = new Bcrypt();

			$sth = $db->prepare("SELECT password FROM users WHERE username = ?");

			$ret = $sth->execute(array($_POST['username']));

			if (!$ret) {
				$err = $db->errorInfo();
				//print_r($err);
				echo json_encode(array("error"=>1, "message"=>$err[2]));	// 2 Is the error message in the returned array
				exit;
			} 
			if ($sth->rowCount() != 1) {
				echo json_encode(array("error"=>1, "message"=>"Username does not exist"));
				exit;
			}
			$pass = $sth->fetchColumn();
			
			$f = fopen("test.txt", "w+");

			fwrite($f, "db: " . $pass . "\nPosted:" . $_POST['password'] . " \nHashed:" . $bcrypt->hash($_POST['password']));

			fclose($f);

			if (!$bcrypt->verify($_POST['password'], $pass)) {
				echo json_encode(array("error"=>1, "message"=>"Invalid Password!"));
				exit;
			}

			echo json_encode(array("error"=>0, "message"=>"User exists"));
		} catch (PDOException $e) {
			echo json_encode(array("error"=>1, "message"=>$e->getMessage()));
		}
		exit;
		break;
	case 2: //Change Password or Username
		exit;
		break;
	case 3: //Deregister
		exit;
		break;
	default:
		echo "Unknown request";
		break;
}

?>