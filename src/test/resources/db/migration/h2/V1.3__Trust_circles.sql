CREATE TABLE ofCertificate (
  id                   VARCHAR(64)     NOT NULL,
  distinguishedName    VARCHAR(255)    NOT NULL,
  serialNumber         VARCHAR(60)     NOT NULL,  
  thumbprint           VARCHAR(60)     NOT NULL,    
  thumbprintAllCaps    VARCHAR(60)     NOT NULL,      
  validStartDate       BIGINT          NOT NULL,
  validEndDate         BIGINT          NOT NULL,  
  certData             BLOB            NOT NULL,  
  domain               VARCHAR(510)    NOT NULL,
  domainAllCaps        VARCHAR(510)    NOT NULL,   
  certStatus           INTEGER         NOT NULL,      
  CONSTRAINT ofCertificate_pk PRIMARY KEY (id)
);
CREATE INDEX ofofCertificate_domainAllCaps_idx ON ofCertificate (domainAllCaps);
CREATE INDEX ofofCertificate_thumbprintAllCaps_idx ON ofCertificate (thumbprintAllCaps);

CREATE TABLE ofTrustAnchor (
  id                   VARCHAR(64)     NOT NULL,
  distinguishedName    VARCHAR(255)    NOT NULL,
  serialNumber         VARCHAR(60)     NOT NULL,  
  thumbprint           VARCHAR(60)     NOT NULL,    
  validStartDate       BIGINT          NOT NULL,
  validEndDate         BIGINT          NOT NULL,  
  anchorData           BLOB            NOT NULL,  
  CONSTRAINT ofTrustAnchor_pk PRIMARY KEY (id)
);

CREATE TABLE ofTrustBundle (
  id                     VARCHAR(64)     NOT NULL,
  bundleName             VARCHAR(255)    NOT NULL,
  bundleURL              VARCHAR(255)    NOT NULL,  
  checkSum               VARCHAR(60),   
  lastRefreshAttempt     BIGINT,
  lastSuccessfulRefresh  BIGINT,  
  lastRefreshError       VARCHAR(30),
  refreshInterval        INTEGER         NOT NULL,    
  signingCertificateData BLOB,
  createTime             BIGINT          NOT NULL, 
  CONSTRAINT ofTrustBundle_pk PRIMARY KEY (id)
);

CREATE TABLE ofTrustBundleAnchor (
  id                   VARCHAR(64)     NOT NULL,
  distinguishedName    VARCHAR(255)    NOT NULL,
  serialNumber         VARCHAR(60)     NOT NULL,  
  thumbprint           VARCHAR(60)     NOT NULL,    
  validStartDate       BIGINT          NOT NULL,
  validEndDate         BIGINT          NOT NULL,  
  anchorData           BLOB            NOT NULL,  
  trustBundleId        VARCHAR(64)     NOT NULL,  
  CONSTRAINT ofTrustBundleAnchor_pk PRIMARY KEY (id)
);
CREATE INDEX ofTrustBundleAnchor_trustBundleId_idx ON ofTrustBundleAnchor (trustBundleId);

CREATE TABLE ofTrustCircle (
  id                     VARCHAR(64)     NOT NULL,
  circleName             VARCHAR(255)    NOT NULL,
  createTime             BIGINT          NOT NULL, 
  CONSTRAINT ofTrustCircles_pk PRIMARY KEY (id)
);

CREATE TABLE ofTrustCircleAnchorReltn (
  trustCircleId          VARCHAR(64)     NOT NULL,
  trustAnchorId          VARCHAR(64)     NOT NULL,
  CONSTRAINT ofTrustCircleAnchorReltn_pk PRIMARY KEY (trustCircleId, trustAnchorId)
);

CREATE TABLE ofTrustCircleBundleReltn (
  trustCircleId          VARCHAR(64)     NOT NULL,
  trustBundleId          VARCHAR(64)     NOT NULL,
  CONSTRAINT ofTrustCircleBundleReltn_pk PRIMARY KEY (trustCircleId, trustBundleId)
);

CREATE TABLE ofTrustCircleDomainReltn (
  trustCircleId          VARCHAR(64)     NOT NULL,
  domainName             VARCHAR(64)     NOT NULL,
  CONSTRAINT ofTrustCircleDomainReltn_pk PRIMARY KEY (trustCircleId, domainName)
);
