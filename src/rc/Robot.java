package rc;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import lejos.pc.comm.NXTComm;
import lejos.pc.comm.NXTCommException;
import lejos.pc.comm.NXTInfo;
import robotambulance.course.Course;
import robotambulance.course.Position;
import robotambulance.course.Vertice;
import robotambulance.util.Constants;
import robotambulance.util.Direction;
import robotambulance.util.Semaphore;

public class Robot implements Runnable {

	private DataInputStream m_dis;
	private DataOutputStream m_dos;
	private final NXTInfo m_nxt;
	private Position position ;
	private Direction direction = null;
	private Position target = null;
	private Course course;
	private int nbOfVictims = 0;
	private Semaphore semOut = new Semaphore(1);
	
	public Robot(NXTInfo _nxt, Position position, Course course) {
		m_nxt = _nxt;
		this.position = position;
		this.course=course;
	}

	public Position getPosition() {
		return position;
	}
	
	

	public Direction getDirection() {
		return direction;
	}
	
	public List<Direction> nextDirection() {
		List<Direction> dirs = null;
		if(target!=null) {
			if(target.getFrom().equals(position.getFrom()) || target.getFrom().equals(position.getTo()) || 
					target.getTo().equals(position.getFrom()) || target.getTo().equals(position.getTo())) {
				
				if(nbOfVictims<Constants.victims_capacity) {
					nbOfVictims++;
					System.out.println(nbOfVictims);
					course.getVictims().remove(0);
				}else {
					nbOfVictims=0;
				}
			}
		}
		if(!course.getVictims().isEmpty()) {
			if(nbOfVictims<Constants.victims_capacity) {
				
				target = course.getVictims().get(0);
				System.out.println(target.toString());
				dirs = course.getCourseForOneDestination(position, target);
			}else {
				Vertice v = course.findHospital();
				target = new Position(v,v.getPredecessor(),0);
				dirs = course.getCourseForOneDestination(position, target);
			}
		
		}
		
		
		return dirs;
		
	}
	

	public void setDirection(Direction direction) {
		this.direction = direction;
	}

	public void setPosition(Position position) {
		this.position = position;
	}

	public boolean connect(NXTComm _comm) throws NXTCommException {
		if (_comm.open(m_nxt)) {
			
			m_dis = new DataInputStream(_comm.getInputStream());
			m_dos = new DataOutputStream(_comm.getOutputStream());
			
		}
		return isConnected();
	}

	public boolean isConnected() {
		return m_dos != null;
	}

	@Override
	public void run() {
		String type;
		try {
			sendPosition();
			while (true) {
				try {
					type = m_dis.readUTF();
					if(type.equals("position")) {
						updatePosition();
						
					} else if(type.equals("direction")) {
						sendDirection();			
					}
				}catch(EOFException e1) {
					break;
				}
				
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	private void sendPosition() throws IOException {
		semOut.acquire();
		m_dos.writeUTF(position.getFrom().getName());
		m_dos.flush();
		m_dos.writeUTF(position.getTo().getName());
		m_dos.flush();
		m_dos.writeUTF(""+position.getDistanceFrom());
		m_dos.flush();
		semOut.release();
	}

	private void sendDirection() throws IOException {
		List<Direction> dirs = nextDirection();
		
		System.out.println(dirs);
		
		semOut.acquire();
		if(dirs!=null) {
			m_dos.writeUTF(dirs.get(0).toString());
			m_dos.flush();
			if(dirs.get(0)==Direction.HALF_TURN) {
				m_dos.writeUTF(dirs.get(1).toString());
				m_dos.flush();
				direction = dirs.get(1);
			}else {
				m_dos.writeUTF("");
				m_dos.flush();
				direction = dirs.get(0);
			}
		}else {
			m_dos.writeUTF("");
			m_dos.flush();
			m_dos.writeUTF("");
			m_dos.flush();	
		}
		semOut.release();
	}

	private void updatePosition() throws IOException {
		Position position = new Position(new Vertice(""),new Vertice(""),0);
		String v1,v2;
		float distanceFrom;
		v1 = m_dis.readUTF();
		v2 = m_dis.readUTF();
		distanceFrom = new Float(m_dis.readUTF());
		
		Vertice from = null;
		Vertice to = null;
		
		position.setFrom(new Vertice(v1));
		position.setTo(new Vertice(v2));
		position.setDistanceFrom(distanceFrom);
		
		for (Iterator<Vertice> iterator = course.getVertices().iterator(); iterator.hasNext() && (from ==null || to == null);) {
			Vertice v = iterator.next();
			if(v.equals(position.getFrom())) {
				position.setFrom(v);				
			}
			if(v.equals(position.getTo())) {
				position.setTo(v);				
			}
		}
		
		
	}

	
}