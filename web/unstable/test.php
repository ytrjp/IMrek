<?php
$x = 100;
while($x) {
echo hash('adler32', uniqid("user", true));
echo '<br />';
$x--;
}
?>