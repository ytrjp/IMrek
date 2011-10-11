<?php

require_once 'config.php';
require_once 'BCrypt.class.php';

function sendReloadSignal() {
	//Get the process ID of the mosquitto broker
	$pid = shell_exec('ps -ef | grep mosquitto | grep -v grep | awk \'{print $2}\'');

	//Use the kill command to send a config reload signal
	shell_exec('/bin/kill -s HUP 3147');
}

// Add a new logging in user to the mosquitto passwords file
function addMqttUser($user, $pass) {
	$exists = false;

	//Split file lines into an array
	$logins = file('/etc/mosquitto/pwfile.pwds', FILE_SKIP_EMPTY_LINES);

	//For each user:password combination
	foreach($logins as $index => $login) {
		$login = explode(":", $login);
		//If the user exists
		if($login[0] == $user) {
			$exists = true;
			$logins[$index] = implode(array($user, ':', $pass));
		}
	}

	if(!$exists) {
		$logins[] = implode(array($user, ':', $pass));
	}
	$file = fopen('/etc/mosquitto/pwfile.pwds', 'a');
	fwrite($file, implode($logins));
	fclose($file);
}

// Generate a session token
function generateToken($len) {
	$str = array();
	$chars = str_split("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789_");
	for ($i = 0; $i < $len; $i++) {
		$str[] = $chars[mt_rand(0, count($chars)-1)];
	}
	return implode($str);
}

// remove a user from the passwords file. This should be called when a user explicitly logs out. 
function removeMqttUser($username) {
	$file = fopen('/etc/mosquitto/pwfile.pwds', 'r');
	$accum = array();
	while (($line = fgets($file))!==false) {
		$username = explode(':', $line);
		if ($username == $username) {
			continue;
		}
		$accum[]=$line;
		
	}
	fclose($file);
	$file = fopen('/etc/mosquitto/pwfile.pwds', 'w');
	fwrite($file, implode('\n', $accum));
	fclose($file);
}

// Generate a token and store it, creating a session for a user
function startSessionForUser($username, &$db) {
	try {
		// Wipe the database of any tokens already associated with this user.
		clearUserSession($username, &$db);

		// Make token and store it with the username in the session table
		$token = generateToken(12);
		$sth = $db->prepare("INSERT INTO session VALUES(?, ?, NULL)");
		$ret = $sth->execute(array($username, $token));
		if (!$ret) {
			$err = $sth->errorInfo();
			return array("status"=>1, "message"=>$err[2]);
		}

		// No errors, session token successfully stored, add user
		addMqttUser($username, $token);
		return array("status"=>0, "message"=>"Session started", "data"=>array("token"=>$token));

	} catch (PDOException $e) {
		return array("status"=>1, "message"=>$e->getMessage());
	}
	
}

// Check if the user is coming in with the same device id as last time
function checkUserDeviceId($username, $deviceid, &$db) {
	try {
		// Check to make sure the deviceid is valid
		if (!isValidDeviceId($deviceid)) {
			return array("status"=>1, "message"=>"Malformed Device Id");
		}

		// grab deviceid and compare it
		$sth = $db->prepare("SELECT deviceid FROM users WHERE username = ?");
		$ret = $sth->execute(array($username));
		if (!$ret) {
			$err = $sth->errorInfo();
			return array("status"=>1, "message"=>$err[2]);
		}

		$dbdeviceid = $sth->fetchColumn();
		if ($dbdeviceid !== $deviceid) {
			return array("status"=>1, "message"=>"Device Id's do not match!");
		}

		return array("status"=>0, "message"=>"Valid");
	} catch (PDOException $e) {
		return array("status"=>1, "message"=>$e->getMessage());
	} 

}

// Check to make sure that a given device ID is actually a device ID and not an invalid spoof
// Device IDs are the hex string of a 64 bit integer that is generated on first boot of an android device
// They should act as a unique identifier for android devices.
function isValidDeviceId($id) {
	// Obviously has to be a string
	if (!is_string($id)) {
		return false;
	}
	// It's a 64bit integer --> hex string so it has to be 16 characters
	if (strlen($id) != 16) {
		return false;
	}

	// Make sure all the characters are actual hex values
	$chars = str_split("0123456789ABCDEF");
	$idchars = str_split(strtoupper($id));
	foreach ($idchars as $ch) {
		if (!in_array($ch, $chars)) {
			return false;
		}
	}
	// It's valid! yay!
	return true;
}

// This functioni will return a boolean value so long as there is not an error
// If there is an error (which will have something to do with the sql), it will return an array
// containing a status and message.
function isSessionTokenValid($username, $token, &$db) {
	global $TOKEN_EXPIRY;
	try {
		$sth = $db->prepare("SELECT username, sesstoken, lastupdate, (UNIX_TIMESTAMP(NOW()) - UNIX_TIMESTAMP(lastupdate)) AS timediff FROM session WHERE username = ? AND sesstoken = ?");
		$ret = $sth->execute(array($username, $token));
		if (!$ret) {
			$err = $sth->errorInfo();
			return array("status"=>1, "message"=>$err[2]);
		}
		// Does this token actually exist?
		if ($sth->rowCount() == 0) {
			return false;
		}

		// look at the time difference given from the select and see if it's greater
		// than the token expire time
		$row = $sth->fetch(PDO::FETCH_ASSOC);
		if ($row["timediff"] > $TOKEN_EXPIRY) {
			return false;
		}

		// It's valid! yay!
		return true;

	} catch (PDOException $e) {
		return array("status"=>1, "message"=>$e->getMessage());
	} 
}

// Check to see if a user is registered or not. 
// returns a boolean to indicate whether the user is registered or not and a
// status array if an error occurs.
function isUserRegistered($username, &$db) {
	try {
		// Look for the user
		$sth = $db->prepare("SELECT username FROM users WHERE username = ?");
		$ret = $sth->execute(array($username));
		if (!$ret) {
			$err = $sth->errorInfo();
			return array("status"=>1, "message"=>$err[2]);
		}

		if ($sth->rowCount() > 0) {		// User exists
			return true;
		}

		return false;

	} catch (PDOException $e) {
		return array("status"=>1, "message"=>$e->getMessage());
	}
}

// Used to change the device id for an existing user logging in on a new device
// returns status array
function setUserDeviceId($username, $deviceid, &$db) {
	try {
		$sth = $db->prepare("UPDATE users SET deviceid = ? WHERE username = ?");
		$ret = $sth->execute(array($deviceid, $username));
		if (!$ret) {		// Whoops...
			$err = $sth->errorInfo();
			return array("status"=>1, "message"=>$err[2]);
		}

		// everythings fine
		return array("status"=>0, "message"=>"DeviceId changed");

	} catch (PDOException $e) {
		return array("status"=>1, "message"=>$e->getMessage());
	}
}

// verify whether a user has the correct password on login
// USE THIS TO ALSO CHECK IF THE USER EXISTS ON LOGIN
// it will return an error if there's no user. this helps reduce db hits
function verifyUserPassword($username, $password, &$db) {
	try {
		$sth = $db->prepare("SELECT password FROM users WHERE username = ?");
		$ret = $sth->execute(array($username));
		if (!$ret) {		// Whoops...
			$err = $sth->errorInfo();
			return array("status"=>1, "message"=>$err[2]);
		}

		// if there's no rows, that user doesn't exist
		if ($sth->rowCount() == 0) {
			return array("status"=>1, "message"=>"No such user");
		}

		// This fetches the only column of what should be the only row. which
		// will contain the password hash
		$passhash = $sth->fetchColumn();

		$bcrypt = new Bcrypt();
		if (!$bcrypt->verify($password, $passhash)) {
			return array("status"=>1, "message"=>"Incorrect password");
		}

		return array("status"=>0, "message"=>"Password is valid");
	} catch (PDOException $e) {
		return array("status"=>1, "message"=>$e->getMessage());
	}
}

// clean temporary user data
function cleanUserData($username, &$db) {
	removeMqttUser($username);
	$check = clearUserSession($username, &$db);
	return $check;
}

function clearUserSession($username, &$db) {
	try {
		$sth = $db->prepare("DELETE FROM session WHERE username = ?");
		$ret = $sth->execute(array($username));
		if (!$ret) {
			$err = $sth->errorInfo();
			return array("status"=>1, "message"=>$err[2]);
		}
		return array("status"=>0, "message"=>"Cleared successfully");		
	} catch (PDOException $e) {
		return array("status"=>1, "message"=>$e->getMessage());
	}
}

?>