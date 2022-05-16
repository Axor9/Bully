package client;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;

public class Gestor {
	static ConcurrentHashMap<Integer, Proceso> procesos = new ConcurrentHashMap<Integer,Proceso>();
	
    public static void main(String[] args) {
    	
    	Set <String> ips = new LinkedHashSet<String>();

        Client client=ClientBuilder.newClient();
        String option;
        String response;
        boolean exit = false;
        
        Scanner lectura = new Scanner (System.in);
        String line = "";  
        String splitBy = ",";  
        
        try {  
	        BufferedReader br = new BufferedReader(new FileReader("Procesos.csv"));  
	        while ((line = br.readLine()) != null){  
	        	String[] split = line.split(splitBy); 
	        	Proceso proceso = new Proceso(Integer.parseInt(split[1]),split[0]);
            	procesos.put(Integer.parseInt(split[1]), proceso);
            	ips.add(split[0]);
	        }  
	        br.close();
        }   
        catch (IOException e){  
        	e.printStackTrace();  
        }    
        
        Collection<Proceso> procesosC = procesos.values();
        
        for(String ipSet : ips) {
        	
        	URI uri=UriBuilder.fromUri("http://"+ ipSet +":8080/Bully").build();
            WebTarget target = client.target(uri);
        	target.path("servicio").path("getIP").request(MediaType.TEXT_PLAIN).get(String.class);
        	
    		System.out.println("Iniciand ip :" + ipSet);
        	for(Proceso proceso : procesosC) {
        		response = target.path("servicio").path("inicio").queryParam("id", proceso.getId()).queryParam("ip", proceso.getIp()).request(MediaType.TEXT_PLAIN).get(String.class);
            	System.out.println(response);
        	}
        }
        
        Gestor gestor = new Gestor();
        
        while(true) {	
            System.out.print("Escriba la siguiente accion: ");
            option = lectura.nextLine();
            option = option.toLowerCase();
            
            String[] opciones = option.split("-");
            opciones[0] = opciones[0].toLowerCase();

            switch(opciones[0]) {
	            case "run":
	                if(opciones[1].equals("all")) {
	                    Collection<Proceso> todos = procesos.values();
	                    for(Proceso proceso:todos) {
	                        gestor.router(String.valueOf(proceso.getId()),"arrancar");
	                    }
	                } else {
	                    String[] ids = opciones[1].split(",");
	                    for(int i=0;i<ids.length;i++) {
	                        gestor.router(ids[i],"arrancar");
	                    }
	                }
	                
	                break;
	            case "stop":
	                if(opciones[1].equals("all")) {
	                    Collection<Proceso> todos = procesos.values();
	                    for(Proceso proceso:todos) {
	                        gestor.router(String.valueOf(proceso.getId()),"parar");
	                    }
	                } else {
	                    String[] ids = opciones[1].split(",");
	                    for(int i=0;i<ids.length;i++) {
	                        gestor.router(ids[i],"parar");
	                    }
	                }
	                
	                break;
            	case "state":
	                if(opciones[1].equals("all")) {
	                    Collection<Proceso> todos = procesos.values();
	                    for(Proceso proceso:todos) {
	                        gestor.router(String.valueOf(proceso.getId()),"estado");
	                    }
	                } else {
	                    String[] ids = opciones[1].split(",");
	                    for(int i=0;i<ids.length;i++) {
	                        gestor.router(ids[i],"estado");
	                    }
	                }
	                break;
                case "exit": 
                	gestor.salir(ips);
                   	exit = true;
                   	break;
                case "restart": 
                	if(opciones[1].equals("all")) {
                		for(String ipSet : ips) {
                    		gestor.reinicio(ipSet);
                    	}
	                } else {
	                	gestor.reinicio(opciones[1]);
	                }
                	
                   	break;
            }
            if(exit) {
            	lectura.close();
            	break;
            }

        }
        
    }
    
    private void reinicio(String ipSet) {
    	Client client=ClientBuilder.newClient();
    	Collection<Proceso> procesosC = procesos.values();
    	boolean flag = false;
    	
    	try {
    		URI uri=UriBuilder.fromUri("http://"+ ipSet +":8080/Bully").build();
            WebTarget target = client.target(uri);
        	target.path("servicio").path("getIP").request(MediaType.TEXT_PLAIN).get(String.class);
            
    		System.out.println("Iniciando ip :" + ipSet);
        	for(Proceso proceso : procesosC) {
        		String response = target.path("servicio").path("inicio").queryParam("id", proceso.getId()).queryParam("ip", proceso.getIp()).request(MediaType.TEXT_PLAIN).get(String.class);
            	System.out.println(response);
        	}
    	} catch(Exception e) {
    		System.out.println("El servidor esta caido");
    	}
    		
	}
    
    public void router(String id,String route) {
    	Proceso proceso = procesos.get(Integer.parseInt(id));
    	
        try {
        	Client client=ClientBuilder.newClient();
            URI uri=UriBuilder.fromUri("http://"+ proceso.getIp() +":8080/Bully").build();
            WebTarget target = client.target(uri);
	    	String response;
	    	response = target.path("servicio").path(route).queryParam("id", id).request(MediaType.TEXT_PLAIN).get(String.class);
	    	System.out.println(response);
	    } catch(Exception e) {
	    	procesos.remove(id);
	    }
    	
    }
    
    public void salir(Set<String> ips) {
    	Client client=ClientBuilder.newClient();
        String response;
    	
    	for(String ip : ips) {
    		try {
	    		URI uri=UriBuilder.fromUri("http://"+ ip +":8080/Bully").build();
	            WebTarget target = client.target(uri);
	        	response = target.path("servicio").path("salir").request(MediaType.TEXT_PLAIN).get(String.class);
	        	System.out.println(response);
	    	} catch(Exception e) {
	    		Collection<Proceso> procesosC = procesos.values();
	    		for(Proceso proceso: procesosC) {
	                if(proceso.getIp().equals(ip)) {
	                    procesos.remove(proceso.getId());
	                }
	            }
	    		System.out.println("Servidor no response");
		    }

    	}
    }
    
}





