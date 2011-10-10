<?php
function sendReloadSignal() {
        $pid = shell_exec('/bin/sh -c "/usr/bin/pkill -SIGHUP mosquitto" 2>&1');
}

sendReloadSignal();
?>
