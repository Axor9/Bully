package services;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URI;
import java.util.Collection;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;

import client.Proceso;

@Path("servicio")
@Singleton
public class Servicio {
	ConcurrentHashMap<Integer, Proceso> procesos=new ConcurrentHashMap<Integer,Proceso>();
	String ip;
	
	@GET
    @Produces(MediaType.TEXT_PLAIN)
    @Path("getIP")
    public void getIP() throws SocketException{
        Enumeration e = NetworkInterface.getNetworkInterfaces();
        NetworkInterface n = (NetworkInterface) e.nextElement();
        Enumeration ee = n.getInetAddresses();
        
        while (ee.hasMoreElements()){
            InetAddress i = (InetAddress) ee.nextElement();
            ip = i.getHostAddress();
        }   
    }

	
	@GET
	@Produces(MediaType.TEXT_PLAIN)
	@Path("inicio")
	public String inicio(@QueryParam(value="id") int id,@QueryParam(value="ip") String ip){
		
		Proceso proceso = new Proceso(id,ip);
		procesos.put(id, proceso);
		
		if(!this.ip.equals(ip)) {
			return "Proceso " + id +" añadido correctamente";
		}
		
		proceso.start();
		return "Proceso " + id +" iniciado correctamente";
		
	}
	
	
	@GET
	@Produces(MediaType.TEXT_PLAIN)
	@Path("parar")
	public String parar(@QueryParam(value="id") int id){
		
		Proceso proceso = procesos.get(id);
		if(proceso.getEstado_proceso().equals("parado")) {
            return "Proceso : " + id + " ya esta parado";
        }
		proceso.parar();
		
		return "Proceso : " + id + " parado";
		
	}
	
	@GET
	@Produces(MediaType.TEXT_PLAIN)
	@Path("arrancar")
	public String arrancar(@QueryParam(value="id") int id){
		Proceso proceso = procesos.get(id);

		if(proceso.getEstado_proceso().equals("arrancado")) {
	           return "Proceso : " + id + " ya esta arrancado";
	    }
		
		synchronized(proceso) {
			proceso.notify();
			
		}
		
		return "Proceso : " + id + " arrancado";
		
	}
	
	@GET
	@Produces(MediaType.TEXT_PLAIN)
	@Path("computar")
	public int computar(@QueryParam(value="id") int id) throws InterruptedException {
		if(!procesos.isEmpty()) {
			if(id > 0) {
				Proceso proceso = procesos.get(id);
				if(!proceso.getIp().equals(this.ip) ) {
					// --------------------------------------------------------------------
			        try {
			        	Client client=ClientBuilder.newClient();
				        URI uri=UriBuilder.fromUri("http://"+ proceso.getIp() +":8080/Bully").build();
				        WebTarget target = client.target(uri);
				        
			        	return target.path("servicio").path("computar").queryParam("id", proceso.getId()).request(MediaType.TEXT_PLAIN).get(int.class);
					} catch(Exception e) {
	                	procesos.remove(proceso.getId());
	                	return -1;
	                }
	                // --------------------------------------------------------------------
				}
				
				return proceso.computar();
			}else {
				return -1;
			}
			
		}else {
			return 0;
		}	
	}
	
	@GET
    @Produces(MediaType.TEXT_PLAIN)
    @Path("eleccion")
    public String eleccion(@QueryParam(value="id") int id){
        Collection<Proceso> mayores = procesos.values();
        String respuestas=null;
        int response;
        
        for(Proceso proceso:mayores) {
            if(proceso.getId() > id && proceso.getEstado_proceso().equals("arrancado")) {     
                // --------------------------------------------------------------------
                try {
                	Client client=ClientBuilder.newClient();
                    URI uri=UriBuilder.fromUri("http://"+proceso.getIp()+":8080/Bully").build();
                    WebTarget target = client.target(uri);
                    
                	response = target.path("servicio").path("ok").queryParam("id", proceso.getId()).request(MediaType.TEXT_PLAIN).get(int.class);
                	if(response > 0) {
                    	respuestas=respuestas + ";" + String.valueOf(response);
                    }
                } catch(Exception e) {
                	String ip = proceso.getIp();
                	for(Proceso proceso2:mayores) {
                		if(proceso2.getIp().equals(ip)) {
                			procesos.remove(proceso2.getId());
                		}
                	}
                }
                // --------------------------------------------------------------------
            }
        }
        return respuestas;
    }

	
	@GET
	@Produces(MediaType.TEXT_PLAIN)
	@Path("ok")
	public int ok(@QueryParam(value="id") int id) throws InterruptedException {
		if(!procesos.isEmpty()) {
			Proceso proceso = procesos.get(id);
			
			if(proceso.getEstado_proceso().equals("parado")) {
				return -1;
			}else {
				return proceso.ok();
			}
		}else {
			return 0;
		}			
	}
	
	@GET
    @Produces(MediaType.TEXT_PLAIN)
    @Path("coordinador")
    public int coordinador(@QueryParam(value="id") int id){
		if(!procesos.isEmpty()) {
			Proceso coord = procesos.get(id);
	        Set<String> ips = new LinkedHashSet<String>();
	        if(coord.getIp().equalsIgnoreCase(this.ip)) {
	            Collection<Proceso> todos = procesos.values();

	            for(Proceso proceso:todos) {
	                if(proceso.getIp().equals(this.ip) && proceso.getEstado_proceso().equals("arrancado")) {
	                    proceso.setCoordinador(id);
	                } else {
	                    ips.add(proceso.getIp());
	                }
	            }
	            
	            for(String ip:ips) {
	                // -------------------------------------------------------------------
	                try {
	                	Client client=ClientBuilder.newClient();
		                URI uri=UriBuilder.fromUri("http://"+ip+":8080/Bully").build();
		                WebTarget target = client.target(uri);
	                	target.path("servicio").path("coordinador").queryParam("id", id).request(MediaType.TEXT_PLAIN).get(String.class);
	                } catch(Exception e) {
	                	ips.remove(ip);
	                	for(Proceso proceso:todos) {
	    	                if(proceso.getIp().equals(this.ip)) {
	    	                    procesos.remove(proceso.getId());
	    	                }
	    	            }
	                	return 0;
	                }
	                // -------------------------------------------------------------------
	            }
	        } else {
	            Collection<Proceso> todos = procesos.values();
	            
	            for(Proceso proceso:todos) {
	                if(proceso.getIp().equals(this.ip) && proceso.getEstado_proceso().equals("arrancado")) {
	                    proceso.setCoordinador(id);
	                }
	            }
	        }
	        return id;
		}else {
			return 0;
		}
    }

	
	@GET
	@Produces(MediaType.TEXT_PLAIN)
	@Path("salir")
	public String salir(){
		Collection<Proceso> procesosC = procesos.values();
		
		
		synchronized(this.getClass()) {
			for(Proceso proceso : procesosC) {
				if(proceso.getIp().equals(this.ip)) {
					proceso.exit();
				}
			}
			procesos.clear();
		}
		
		return "Todos los procesos han muerto";
		
	}
	
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Path("estado")
    public String estado(@QueryParam(value="id") int id){
        
        return "Proceso " + id + ": estado "+procesos.get(id).getEstado_proceso()+ " coordinador : "+ procesos.get(id).getCoordinador();
        
    }

	
}
