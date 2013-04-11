package SDEngine;

import java.util.ArrayList;

import org.lwjgl.openal.AL;
import org.newdawn.slick.openal.SoundStore;

public class SDAudio {
  ArrayList<SDSound> _sounds = null;

  public SDAudio() throws SDException {
    try {
      _sounds = new ArrayList<SDSound>();
      SoundStore.get().init();
    } catch (Exception e) {
      throw new SDException();
    }
    if (!SoundStore.get().soundWorks()) {
      throw new SDException();
    }

    SDLog.out("SDAudio()");
  }

  public boolean init() {
    return true;
  }

  public void destroy() {
    if (_sounds != null) {
      for (SDSound temp : _sounds) {
        temp.stop();
        temp = null;
      }
      _sounds.clear();
      _sounds = null;
    }
    AL.destroy();
  }

  public void clear() {
    if (_sounds != null) {
      stopAll();
      _sounds.clear();
    }
    SoundStore.get().clear();
  }

  public void update() {
    SoundStore.get().poll(0);
  }

  public void load(String path) {
    if (_sounds != null) {
      String parts[] = path.split("\\.");
      String type = parts[parts.length - 1].toUpperCase();
      String name = parts[0];
      _sounds.add(new SDSound(type, path, name));
    }
  }

  public void play(int id, float volume) {
    for (SDSound temp : _sounds) {
      if (temp._id == id) {
        temp.play(volume);
        return;
      }
    }
  }

  public void play(String name, float volume) {
    for (SDSound temp : _sounds) {
      if (temp._name.equals(name)) {
        temp.play(volume);
        return;
      }
    }
  }

  public void play(String name) {
    play(name, 1.0f);
  }

  public void play(int id) {
    play(id, 1.0f);
  }

  public void stop(String name) {
    if (_sounds != null) {
      for (SDSound temp : _sounds) {
        if (name.equals(temp._name))
          temp.stop();
      }
    }
  }

  public void stop(int id) {
    if (_sounds != null) {
      for (SDSound temp : _sounds) {
        if (id == temp._id)
          temp.stop();
      }
    }
  }

  public void stopAll() {
    if (_sounds != null) {
      for (SDSound temp : _sounds)
        temp.stop();
    }
  }
}
