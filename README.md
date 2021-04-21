# ATS-Tool
Tool to display the ATS and communication parameter of ISO 14443 cards.

Supposed to work with Identiv uTrust / CLOUD 47x0 F dual interface reader.

##Build

Use

    mvn clean package

to create jar in target folder.

##Using

Call with

    java -jar target/ats-tool-1.0.jar -l

to show a list of readers

    Available card terminals:
     Identive Identive CLOUD 4500 F Dual Interface Reader [uTrust 4700 F Contact Reader] (53201519204084) 00 00
     Identive Identive CLOUD 4500 F Dual Interface Reader [uTrust 4700 F CL Reader] (53201519204084) 01 00

Select a reader with

    java -jar target/ats-tool-1.0.jar -r "Identive Identive CLOUD 4500 F Dual Interface Reader [uTrust 4700 F CL Reader] (53201519204084) 01 00"

displays

    Using reader "Identive Identive CLOUD 4500 F Dual Interface Reader [uTrust 4700 F CL Reader] (53201519204084) 01 00"
    Reader to card rates 106,212,424,848
    Card to reader rates 106,212,424,848
    Different rates in both direction
    T=CL ISO 14443-4 card (Type A)
    ATS: 13787791028031815448534D31738021408107
    Rates reader:card 848Kbps:848Kbps
