ALTER TABLE INNTEKTSMELDING
ADD COLUMN INNSENDINGS_AARSAK VARCHAR(100) NOT NULL;

comment on column INNTEKTSMELDING.INNSENDINGS_AARSAK is 'Om inntektsmeldingen er ny eller endret';
