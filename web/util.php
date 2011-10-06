<?php

function sendReloadSignal() {
	//Get the process ID of the mosquitto broker
	$pid = shell_exec('ps -ef | grep mosquitto | grep -v grep | awk \'{print $2}\'');

	//Use the kill command to send a config reload signal
	shell_exec('/bin/kill -s HUP 3147');
}

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

?>