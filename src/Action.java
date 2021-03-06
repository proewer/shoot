import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.swing.ImageIcon;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.Timer;

public class Action extends JPanel implements MouseListener, MouseMotionListener {

  private static final long serialVersionUID = 1L;

  private boolean running = false;

  private Image background = ( new ImageIcon( getClass().getResource( "resource/fm.png" ) ) ).getImage();

  private Image background2 = ( new ImageIcon( getClass().getResource( "resource/bg.png" ) ) ).getImage();

  private ImageIcon cursor = new ImageIcon( getClass().getResource( "resource/aim.png" ) );

  private ImageIcon bullet = new ImageIcon( getClass().getResource( "resource/bullet.png" ) );

  private Image shell = new ImageIcon( getClass().getResource( "resource/shell.png" ) ).getImage();

  private int shellCount;

  private int xMousePosition;

  private int yMousePosition;

  private List<Demand> demands;

  private List<Coin> coins;

  private Question question;

  private Bomb bomb;

  private Mail mail;

  private int balance;

  private static final int DEMANDS = 15;

  private static final int COINS = 3;

  private Timer timer;

  private int time;

  // TODO: set high score file, for examle a network path
  private File highScore = new File( "hs.dat" );

  private List<String> scores;

  private boolean readScore = true;

  private boolean writeScore = true;

  private double shoots;

  private double hits;

  private DecimalFormat df = new DecimalFormat( "#.##" );

  public Action() {
    addMouseListener( this );
    addMouseMotionListener( this );
  }

  @Override
  protected void paintComponent( Graphics g ) {
    super.paintComponent( g );
    // preview screen
    if ( !running ) {
      g.drawImage( background2, 0, 0, null );
      if ( readScore )
        readScore = readScore();
      g.setFont( new Font( "TimesRoman", Font.PLAIN, 30 ) );
      g.setColor( Color.BLACK );
      g.drawString( "High Score", 10, 140 );
      for ( int i = 1; i <= 10; i++ ) {
        if ( scores.size() < i )
          break;
        String[] temp = scores.get( i - 1 ).split( "#" );
        g.drawString( i + ". " + temp[0] + " " + temp[1] + " " + temp[2] + "%", 10, 100 + 50 * ( i + 1 ) );
      }
      g.setColor( Color.RED );
      g.drawString( "Let's get ready to rumble! Click!", 480, 400 );
      return;
    }
    // running game
    // background
    g.drawImage( background, 0, 0, null );
    // cursor
    cursor.paintIcon( this, g, xMousePosition, yMousePosition );
    // demands and coins + bomb
    for ( Demand demand : demands )
      demand.draw( g );
    for ( Coin coin : coins ) {
      coin.draw( g );
    }
    if ( question != null )
      question.draw( g );
    if ( bomb != null )
      bomb.draw( g );
    if ( mail != null )
      mail.draw( g );
    // shell
    int xShell = 1050;
    for ( int i = 0; i < shellCount; i++ ) {
      g.drawImage( shell, xShell, 550, null );
      xShell += 50;
    }
    // status
    if ( balance >= 0 )
      g.setColor( Color.GREEN );
    else
      g.setColor( Color.RED );
    g.setFont( new Font( "TimesRoman", Font.PLAIN, 50 ) );
    g.drawString( balance + "�", 1180, 80 );
    g.setColor( Color.BLUE );
    g.drawString( String.format( "%02d:%02d",
        TimeUnit.MILLISECONDS.toMinutes( time ) - TimeUnit.HOURS.toMinutes( TimeUnit.MILLISECONDS.toHours( time ) ),
        TimeUnit.MILLISECONDS.toSeconds( time )
            - TimeUnit.MINUTES.toSeconds( TimeUnit.MILLISECONDS.toMinutes( time ) ) ),
        900, 80 );
    // end
    if ( time <= 0 ) {
      running = false;
      time = 0;
      timer.stop();
      String tq = df.format( hits / shoots * 100 ).replaceAll( "\\?", "0.0" );
      String name = JOptionPane.showInputDialog( this,
          "balance: " + balance + " hit rate: " + tq + "% name?" );
      if ( writeScore )
        writeScore = writeScore( name, balance, tq );
    }
  }

  private boolean readScore() {
    scores = new ArrayList<>();
    try ( BufferedReader reader = new BufferedReader( new FileReader( highScore ) ) ) {
      String line = reader.readLine();
      while ( line != null ) {
        scores.add( line );
        line = reader.readLine();
      }
      reader.close();
    }
    catch ( Exception e ) {
      return false;
    }

    scores.sort( new Comparator<String>() {
      @Override
      public int compare( String o1, String o2 ) {
        String[] temp1 = o1.split( "#" );
        String[] temp2 = o2.split( "#" );
        Integer t1 = Integer.parseInt( temp1[1] );
        Integer t2 = Integer.parseInt( temp2[1] );
        Double q1 = Double.parseDouble( temp1[2].replaceAll( ",", "." ) );
        Double q2 = Double.parseDouble( temp2[2].replaceAll( ",", "." ) );
        return t1.equals( t2 ) ? q1.compareTo( q2 ) * -1 : t1.compareTo( t2 ) * -1;
      }
    } );
    return true;
  }

  private boolean writeScore( String name, int score, String tq ) {
    try ( BufferedWriter output = new BufferedWriter( new FileWriter( highScore, true ) ) ) {
      output.append( name + "#" + score + "#" + tq );
      output.newLine();
      output.close();
    }
    catch ( Exception e ) {
      return false;
    }
    return true;
  }

  @Override
  public Dimension getPreferredSize() {
    return new Dimension( App.D_W, App.D_H );
  }

  @Override
  public void mouseMoved( MouseEvent e ) {
    xMousePosition = e.getX();
    yMousePosition = e.getY();
    repaint();
  }

  @Override
  public void mouseClicked( MouseEvent e ) {
    if ( !running ) {
      running = true;
      demands = new ArrayList<>();
      coins = new ArrayList<>();
      for ( int i = 0; i < DEMANDS; i++ )
        demands.add( new Demand() );
      for ( int i = 0; i < COINS; i++ )
        coins.add( new Coin() );
      question = new Question();
      balance = 0;
      shellCount = 5;
      shoots = 0;
      hits = 0;
      time = 60000;
      int bombTime = Demand.getRandomInt( 10000, 60000 );
      int mailTime = Demand.getRandomInt( 10000, 60000 );
      timer = new Timer( 50, new ActionListener() {
        @Override
        public void actionPerformed( ActionEvent ae ) {
          question.move();
          for ( Demand demand : demands ) {
            balance += demand.move();
            repaint();
          }
          for ( Coin coin : coins ) {
            balance += coin.move();
            repaint();
          }
          question.move();
          repaint();
          if ( time >= bombTime && bombTime >= time - 50 ) {
            bomb = new Bomb();
          }
          if ( bomb != null ) {
            if ( bomb.move() == -1 )
              bomb = null;
            repaint();
          }
          if ( time >= mailTime && mailTime >= time - 50 ) {
            mail = new Mail();
          }
          else if ( time >= mailTime - 5000 && mailTime - 5000 >= time - 50 ) {
            mail = null;
          }
          if ( mail != null ) {
            repaint();
          }
          time -= 50;
        }
      } );
      timer.start();
    }
  }

  @Override
  public void mousePressed( MouseEvent e ) {
    if ( !running )
      return;
    if ( e.getButton() == MouseEvent.BUTTON1 ) {
      if ( shellCount > 0 ) {
        Thread t = new Thread( new SoundBox( SoundEffect.SHOOT ) );
        t.start();
        shellCount -= 1;
        int x = e.getX();
        int y = e.getY();
        int tolerance = 20;
        boolean match = false;
        for ( Demand demand : demands ) {
          if ( demand.x - tolerance <= x && x <= demand.x + tolerance &&
              demand.y - tolerance <= y && y <= demand.y + tolerance ) {
            bullet.paintIcon( getComponentPopupMenu(), getGraphics(), x, y );
            demand.start();
            match = true;
          }
        }
        for ( Coin coin : coins ) {
          if ( coin.x - tolerance <= x && x <= coin.x + tolerance &&
              coin.y - tolerance <= y && y <= coin.y + tolerance ) {
            bullet.paintIcon( getComponentPopupMenu(), getGraphics(), x, y );
            coin.start();
            balance += 50;
            match = true;
          }
        }
        if ( question != null ) {
          if ( question.x - tolerance <= x && x <= question.x + tolerance &&
              question.y - tolerance <= y && y <= question.y + tolerance ) {

            question.start();
            int i = Demand.getRandomInt( 0, 100 );
            // TODO: Random-Ereignis
            if ( i < 50 ) {
              Thread t2 = new Thread( new SoundBox( SoundEffect.WIN ) );
              t2.start();
              time = time + 5000;
              for ( Demand d : demands ) {
                d.slower();
              }
            }
            else if ( i >= 50 ) {
              Thread t2 = new Thread( new SoundBox( SoundEffect.ERROR ) );
              t2.start();
              time = time - 5000;
              for ( Demand d : demands ) {
                d.faster();
              }
            }

            match = true;
          }
        }
        if ( bomb != null ) {
          if ( bomb.x - tolerance <= x && x <= bomb.x + tolerance &&
              bomb.y - tolerance <= y && y <= bomb.y + tolerance ) {
            for ( Demand demand : demands ) {
              demand.start();
            }
            for ( Coin coin : coins ) {
              coin.start();
            }
            question.start();
            Thread t2 = new Thread( new SoundBox( SoundEffect.BOOM ) );
            t2.start();
            bomb = null;
            mail = null;
            match = true;
          }
        }
        if ( match ) {
          hits++;
          shoots++;
        }
        else {
          shoots++;
        }
      }
      else {
        Thread t = new Thread( new SoundBox( SoundEffect.DRY ) );
        t.start();
      }
    }
    else if ( e.getButton() == MouseEvent.BUTTON3 ) {
      Thread t = new Thread( new SoundBox( SoundEffect.RELOAD ) );
      t.start();
      shellCount = 5;
    }
  }

  @Override
  public void mouseReleased( MouseEvent e ) {
  }

  @Override
  public void mouseEntered( MouseEvent e ) {
  }

  @Override
  public void mouseExited( MouseEvent e ) {
  }

  @Override
  public void mouseDragged( MouseEvent e ) {
  }

}
