<?php

require_once 'config.php';
require 'util.php';
require_once 'BCrypt.class.php';

if (!array_key_exists('action', $_POST)) {
	echo json_encode(array("status"=>1, "message"=>"Invalid parameters"));
	exit;
}

switch(intval($_POST['action'])) {
	case 0:							// Register
		// Make sure we have all tha paramters
		if (!isset($_POST['username']) || !isset($_POST['password']) || !isset($_POST['deviceid'])) {
			echo json_encode(array("status"=>1, "message"=>"Invalid parameters"));
			exit;
		}

		$usernamelen = strlen($_POST['username']);

		// Make sure the username isn't too short
		if ($usernamelen < 5) {
			echo json_encode(array("status"=>1, "message"=>"Username is too short. Must be at least 5 characters"));
			exit;
		}
		// Make sure the username will fit in the db field
		if ($usernamelen > 12) {
			echo json_encode(array("status"=>1, "message"=>"Username is too long. Please limit it to 12 characters"));
			exit;
		}

		// Make sure the username contains only valid characters
		$chars = str_split("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890_");
		for ($i = 0; $i < $usernamelen; $i++) {
			if (!in_array($_POST['username']{$i}, $chars)) {
				echo json_encode(array("status"=>1, "message"=>"Invalid characters in username. Valid characters are alphanumeric and underscores ( _ )"));
				exit;
			}
		}

		// Make sure password is not too short
		if (strlen($_POST['password']) < 6) {
			echo json_encode(array("status"=>1, "message"=>"Password is too short. Must be at least 6 characters"));
			exit;
		}

		// Make sure password is not too long
		if (strlen($_POST['password']) > 12) {
			echo json_encode(array("status"=>1, "message"=>"Password is too long. Must be at under 12 characters"));
			exit;
		}

		// Check if we have a valid deviceid
		if (!isValidDeviceId($_POST['deviceid'])) {
			echo json_encode(array("status"=>1, "message"=>"Malformed device id"));
			exit;
		}

		// Hash the password
		$bcrypt = new Bcrypt();
		$passwordHash = $bcrypt->hash($_POST['password']);

		try {
			$db = new PDO("mysql:host=".$DB_HOST.";dbname=".$DB_NAME,$DB_USER,$DB_PASS);

			// Make sure the user doesn't already exist
			$reg = isUserRegistered($_POST['username'],&$db);

			// if it's an array, there was an error
			if (is_array($reg)) {
				echo json_encode($reg);
				exit;
			}

			// If the username is already registered return an error
			if ($reg) {
				echo json_encode(array("status"=>1, "message"=>"Username already in use"));
				exit;
			}

			// check device id
			$check = isValidDeviceId($_POST['deviceid']);
			if (!$check) {
				echo json_encode(array("status"=>1, "message"=>"Malformed device id"));
				exit;
			}

			// Prepare to add the user to the database
			$sth = $db->prepare("INSERT INTO users VALUES(?, ?, ?)");
			$ret = $sth->execute(array($_POST['username'], $passwordHash, $_POST['deviceid']));
			// Make sure there wasn't an error
			if (!$ret) {
				$err = $sth->errorInfo();
				echo json_encode(array("status"=>1, "message"=>$err[2]));
				exit;
			}

			// Yay it worked! 
			//echo json_encode(array("status"=>0, "message"=>"Registration successful!")); 			need to start the session
			$check = startSessionForUser($_POST['username'], &$db);
			echo json_encode($check);
			exit;

		} catch (PDOException $e) {
			echo json_encode(array("status"=>1, "message"=>$e->getMessage()));
			exit;
		}

		break;
	case 1:							// login
		
		// Make sure we have all tha paramters
		if (!isset($_POST['username']) || !isset($_POST['password']) || !isset($_POST['deviceid'])) {
			echo json_encode(array("status"=>1, "message"=>"Invalid parameters"));
			exit;
		}

		$usernamelen = strlen($_POST['username']);

		// Make sure the username isn't too short
		if ($usernamelen < 5) {
			echo json_encode(array("status"=>1, "message"=>"Username is too short. Must be at least 5 characters"));
			exit;
		}
		// Make sure the username will fit in the db field
		if ($usernamelen > 12) {
			echo json_encode(array("status"=>1, "message"=>"Username is too long. Please limit it to 12 characters"));
			exit;
		}

		// Make sure password is not too short
		if (strlen($_POST['password']) < 6) {
			echo json_encode(array("status"=>1, "message"=>"Password is too short. Must be at least 6 characters"));
			exit;
		}

		// Make sure password is not too long
		if (strlen($_POST['password']) > 12) {
			echo json_encode(array("status"=>1, "message"=>"Password is too long. Must be at under 12 characters"));
			exit;
		}

		// Check if we have a valid deviceid
		if (!isValidDeviceId($_POST['deviceid'])) {
			echo json_encode(array("status"=>1, "message"=>"Malformed device id"));
			exit;
		}
		// Information in valid format -- we can check the pass hash hit the database now, yeah, hit it!

		try {
			$db = new PDO("mysql:host=".$DB_HOST.";dbname=".$DB_NAME,$DB_USER,$DB_PASS);

			// verify the password. Also checks if the user exists
			$valid = verifyUserPassword($_POST['username'], $_POST['password'], &$db);
			if ($valid["status"] == 1) {
				echo json_encode($valid);
				exit;
			}

			// Check if it's the same device id
			$valid = checkUserDeviceId($_POST['username'], $_POST['deviceid'], &$db);
			if ($valid['status'] == 1) {
				// echo json_encode($valid);
				// exit;						instead of exiting we update the device id

				$check = setUserDeviceId($_POST['username'], $_POST['deviceid'], &$db);
				if ($check['status'] == 1) {
					echo json_encode($check);
					exit;
				}
			}

			// Make sure we clear out old sessions.
			$check = cleanUserData($_POST['username'], &$db);
			if ($check['status'] == 1) {
				echo json_encode($check);
				exit;
			}

			// start the session
			$check = startSessionForUser($_POST['username'], &$db);
			echo json_encode($check);
			exit;
		} catch (PDOException $e) {
			echo json_encode(array("status"=>1, "message"=>$e->getMessage()));
			exit;
		} 

		break;
	case 2:							// Reconnect
		// Make sure we have all tha paramters
		if (!isset($_POST['username']) || !isset($_POST['deviceid']) || !isset($_POST['token'])) {
			echo json_encode(array("status"=>1, "message"=>"Invalid parameters"));
			exit;
		}

		$usernamelen = strlen($_POST['username']);

		// Make sure the username isn't too short
		if ($usernamelen < 5) {
			echo json_encode(array("status"=>1, "message"=>"Username is too short. Must be at least 5 characters"));
			exit;
		}
		// Make sure the username will fit in the db field
		if ($usernamelen > 20) {
			echo json_encode(array("status"=>1, "message"=>"Username is too long. Please limit it to 20 characters"));
			exit;
		}

		// Check if we have a valid deviceid
		if (!isValidDeviceId($_POST['deviceid'])) {
			echo json_encode(array("status"=>1, "message"=>"Malformed device id"));
			exit;
		}

		try {
			$db = new PDO("mysql:host=".$DB_HOST.";dbname=".$DB_NAME,$DB_USER,$DB_PASS);

			// User can only reconnect if it's from the same device. otherwise h4x
			$check = checkUserDeviceId($_POST['username'], $_POST['deviceid'], &$db);
			if ($check['status'] == 1) {
				echo json_encode($check);
				exit;
			}

			// check if the token is valid
			$check = isSessionTokenValid($_POST['username'],$_POST['token'], &$db);
			// if it's an array, there was an error
			if (is_array($check)) {
				echo json_encode($check);
				exit;
			}
			// if it's false, the token has expired
			if (!$check) {
				echo json_encode(array("status"=>1, "message"=>"Token expired"));
				exit;
			}
			// Yay we're good to go!
			echo json_encode(array("status"=>0, "message"=>"Ok to connect"));
			exit;

		} catch (PDOException $e) {
			echo json_encode(array("status"=>1, "message"=>$e->getMessage()));
			exit;
		} 
		
		break;
	case 3: 						//Change Password or Username
		exit;
		break;
	case 4: 						//Deregister
		exit;
		break;
	default:
		echo json_encode(array("status"=>1, "message"=>"Unknown Request"));
		break;

}


