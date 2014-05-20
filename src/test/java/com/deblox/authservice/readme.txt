    vertx run Server.java -cp ../../../../../../  -cluster  
    vertx run RateCounter.java -cluster -cluster-port 8902
    vertx run Client.java -cluster -cluster-port 8005 -instances 8
