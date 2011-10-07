CREATE DATABASE imrek;


USE imrek;

CREATE TABLE users (
	username VARCHAR(20),
	password VARCHAR(255),
	deviceid VARCHAR(16)
);

CREATE TABLE session (
	username VARCHAR(20),
	sesstoken VARCHAR(30),
	lastupdate TIMESTAMP DEFAULT NOW()
);