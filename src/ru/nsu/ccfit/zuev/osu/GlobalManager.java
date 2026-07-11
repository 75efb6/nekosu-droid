package ru.nsu.ccfit.zuev.osu;

import android.util.DisplayMetrics;

import java.lang.ref.WeakReference;

import org.anddev.andengine.engine.Engine;
import org.anddev.andengine.engine.camera.Camera;

import ru.nsu.ccfit.zuev.audio.serviceAudio.SaveServiceObject;
import ru.nsu.ccfit.zuev.audio.serviceAudio.SongService;
import ru.nsu.ccfit.zuev.osu.editor.EditorScene;
import ru.nsu.ccfit.zuev.osu.game.GameScene;
import ru.nsu.ccfit.zuev.osu.game.GlobalFPSOverlay;
import ru.nsu.ccfit.zuev.osu.menu.SongMenu;
import ru.nsu.ccfit.zuev.osu.scoring.ScoreLibrary;
import ru.nsu.ccfit.zuev.osu.scoring.ScoringScene;

/**
 * Created by Fuuko on 2015/4/24.
 */
public class GlobalManager {
    private static GlobalManager instance;
    private Engine engine;
    private Camera camera;
    private GameScene gameScene;
    private MainScene mainScene;
    private ScoringScene scoring;
    private SongMenu songMenu;
    private EditorScene editorScene;
    private WeakReference<MainActivity> mainActivityRef;
    private int loadingProgress;
    private String info;
    private SongService songService;
    private TrackInfo selectedTrack;
    private SaveServiceObject saveServiceObject;
    private String skinNow;

    public static GlobalManager getInstance() {
        if (instance == null) {
            instance = new GlobalManager();
        }
        return instance;
    }

    public TrackInfo getSelectedTrack() {
        return selectedTrack;
    }

    public void setSelectedTrack(TrackInfo selectedTrack) {
        this.selectedTrack = selectedTrack;
    }

    public void init() {
        final MainActivity activity = getMainActivity();
        saveServiceObject = (SaveServiceObject) activity.getApplication();
        songService = saveServiceObject.getSongService();
        setLoadingProgress(10);
        setMainScene(new MainScene());
        getMainScene().load(activity);
        setInfo("Loading skin...");
        skinNow = Config.getSkinPath();
        ResourceManager.getInstance().loadSkin(skinNow);
        ScoreLibrary.getInstance().load(activity);
        setLoadingProgress(20);
        PropertiesLibrary.getInstance().load(activity);
        setLoadingProgress(30);
        setGameScene(new GameScene(getEngine()));
        setSongMenu(new SongMenu());
        setLoadingProgress(40);
        getSongMenu().init(activity, getEngine(), getGameScene());
        getSongMenu().load();
        setScoring(new ScoringScene(getEngine(), getGameScene(), getSongMenu()));
        getSongMenu().setScoringScene(getScoring());
        getGameScene().setScoringScene(getScoring());
        getGameScene().setOldScene(getSongMenu().getScene());

        new GlobalFPSOverlay().attachToCamera(camera);

        if (songService != null) {
            songService.stop();
            songService.hideNotification();
        }
    }

    public Engine getEngine() {
        return engine;
    }

    public void setEngine(Engine engine) {
        this.engine = engine;
    }

    public String getSkinNow() {
        return skinNow;
    }

    public void setSkinNow(String skinNow) {
        this.skinNow = skinNow;
    }

    public Camera getCamera() {
        return camera;
    }

    public void setCamera(Camera camera) {
        this.camera = camera;
    }

    public GameScene getGameScene() {
        return gameScene;
    }

    public void setGameScene(GameScene gameScene) {
        this.gameScene = gameScene;
    }

    public MainScene getMainScene() {
        return mainScene;
    }

    public void setMainScene(MainScene mainScene) {
        this.mainScene = mainScene;
    }

    public ScoringScene getScoring() {
        return scoring;
    }

    public void setScoring(ScoringScene scoring) {
        this.scoring = scoring;
    }

    public SongMenu getSongMenu() {
        return songMenu;
    }

    public void setSongMenu(SongMenu songMenu) {
        this.songMenu = songMenu;
    }

    public EditorScene getEditorScene() {
        return editorScene;
    }

    public void setEditorScene(EditorScene editorScene) {
        this.editorScene = editorScene;
    }

    public MainActivity getMainActivity() {
        return mainActivityRef != null ? mainActivityRef.get() : null;
    }

    public void setMainActivity(MainActivity mainActivity) {
        this.mainActivityRef = new WeakReference<>(mainActivity);
    }

    public int getLoadingProgress() {
        return loadingProgress;
    }

    public void setLoadingProgress(int loadingProgress) {
        this.loadingProgress = loadingProgress;
    }

    public String getInfo() {
        return info;
    }

    public void setInfo(String info) {
        this.info = info;
    }

    public SongService getSongService() {
        return songService;
    }

    public void setSongService(SongService songService) {
        this.songService = songService;
    }

    public SaveServiceObject getSaveServiceObject() {
        return saveServiceObject;
    }

    public void setSaveServiceObject(SaveServiceObject saveServiceObject) {
        this.saveServiceObject = saveServiceObject;
    }

    public DisplayMetrics getDisplayMetrics() {
        final DisplayMetrics dm = new DisplayMetrics();
        final MainActivity activity = getMainActivity();
        if (activity != null) {
            activity.getWindowManager().getDefaultDisplay().getMetrics(dm);
        }
        return dm;
    }
}
