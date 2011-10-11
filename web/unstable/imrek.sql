CREATE DATABASE imrek;


USE imrek;

CREATE TABLE users (
	username VARCHAR(12),
	password VARCHAR(60),
	deviceid VARCHAR(16)
);

CREATE TABLE session (
	username VARCHAR(12),
	sesstoken VARCHAR(12),
	lastupdate TIMESTAMP DEFAULT NOW()
);