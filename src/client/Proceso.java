package client;

import java.net.URI;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;

public class Proceso extends Thread{
	int id;
	String ip;
	int coordinador = -1;
	String estado_eleccion = "eleccion_activa";
	String estado_proceso = "parado";
	boolean exit = true;
	
	public Proceso(int id,String ip) {
		this.id = id;
		this.ip = ip;
	}
	
	public long getId () {
		return this.id;
	}
	
	public void setId (int id) {
		this.id = id;
	}
	
	public String getIp () {
		return this.ip;
	}
	
	public void setIp (String ip) {
		this.ip = ip;
	}
	
	public String getEstado_proceso() {
		return this.estado_proceso;
	}
	
	public String getEstado_eleccion() {
		return this.estado_eleccion;
	}
	
	public long getCoordinador() {
		return this.coordinador;
	}
	
	public void setCoordinador(int coordinador) {
		this.coordinador = coordinador;
	}
	
		
	public void run() {
		int valor = 0;
		Client client=ClientBuilder.newClient();
        URI uri=UriBuilder.fromUri("http://localhost:8080/Bully").build();
        WebTarget target = client.target(uri);
        
		while(exit) {
			if(this.estado_proceso.equals("parado")) {
				try {
					synchronized(this) {
						this.wait();
						this.estado_proceso = "arrancado";	
					}
					eleccion();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}else {
				System.out.println("Mi id: " + this.id+ " y mi coordinador es " + this.coordinador);
				try {
					Thread.sleep((long) ((double) (Math.random()*5+5)*100));
					valor = target.path("servicio").path("computar").queryParam("id", coordinador).request(MediaType.TEXT_PLAIN).get(int.class);
					if(valor <= 0) {
						this.estado_eleccion = "eleccion_activa";
						eleccion();
					}
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}
	
	public void parar() {
		this.coordinador=-1;
		this.estado_proceso = "parado";
	}
	
	public int computar() throws InterruptedException {
		synchronized(this.getClass()) {
			if(this.estado_proceso.equals("parado")) {
				return -1;
			}else {
				Thread.sleep((long) ((double) (Math.random()*2+1)*100));
				return 1;
			}
		}
	}
	
	public void eleccion() throws InterruptedException{
		this.coordinador = -1;
		while(true) {
	        Client client=ClientBuilder.newClient();
	        URI uri=UriBuilder.fromUri("http://"+this.ip+":8080/Bully").build();
	        WebTarget target = client.target(uri);
	                
	        String response = target.path("servicio").path("eleccion").queryParam("id", id).request(MediaType.TEXT_PLAIN).get(String.class);
	        if(response.isEmpty()) {
	        	this.coordinador = this.id;
	        	target.path("servicio").path("coordinador").queryParam("id", this.coordinador).request(MediaType.TEXT_PLAIN).get(int.class);
	        	break;
	        }else {
	        	this.estado_eleccion = "eleccion_pasiva";
	        	Thread.sleep(1000);
	        	if(this.coordinador >= 0) {
	        		break;
	        	}
	        }
		}
    }

	
	public int ok() throws InterruptedException {
		eleccion();
		return this.id;
	}
	
	public int exit() {
		this.exit = false;
		return 1;
	}
	 
	
	
	

}
