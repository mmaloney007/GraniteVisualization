package SDEngine;

import org.newdawn.slick.openal.Audio;
import org.newdawn.slick.openal.AudioLoader;
import org.newdawn.slick.util.ResourceLoader;

import static SDEngine.SDLog.*;

public class SDSound {
  String _path, _type, _name;
  int _id;
  Audio _audio;

  public SDSound(String type, String path, String name) {
    _path = path;
    _type = type;
    _name = name;
    try {
      _audio = AudioLoader.getAudio(type,
          ResourceLoader.getResourceAsStream(path));
      _id = _audio.getBufferID();
    } catch (Exception e) {
      err("failed to load " + type + ": " + path);
    }
  }

  public int playAsMusic(float pitch, float gain, boolean loop) {
    return _audio.playAsMusic(pitch, gain, loop);
  }

  public int playAsMusic() {
    return playAsMusic(1.0f, 1.0f, true);
  }

  public int playAsSoundEffect(float pitch, float gain, boolean loop, float x,
      float y, float z) {
    return _audio.playAsSoundEffect(pitch, gain, loop, x, y, z);
  }

  public int playAsSoundEffect(float pitch, float gain, boolean loop) {
    return _audio.playAsSoundEffect(pitch, gain, loop);
  }

  public int play(float volume) {
    return playAsSoundEffect(1.0f, volume, false);
  }

  public int play() {
    return playAsSoundEffect(1.0f, 1.0f, false);
  }

  public void setPosition(float position) {
    _audio.setPosition(position);
  }

  public void stop() {
    _audio.stop();
  }

  public boolean isPlaying() {
    return _audio.isPlaying();
  }
}
