CREATE TABLE ofProxyCredentials (
  subject              VARCHAR(64)     NOT NULL,
  secretHash           BLOB            NOT NULL,
  creationDate         VARCHAR(15)     NOT NULL,
  CONSTRAINT ofProxyCredentials_pk PRIMARY KEY (subject)
);