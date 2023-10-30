package tanks.gui.screen.leveleditor;

import tanks.Drawing;
import tanks.GameObject;
import tanks.editorselector.StringSelector;
import tanks.gui.Button;
import tanks.gui.TextBox;
import tanks.gui.screen.Screen;

public class OverlaySelectString extends ScreenLevelEditorOverlay
{
    public StringSelector<?> selector;
    public TextBox textBox = new TextBox(this.centerX, this.centerY, this.objWidth * 2, this.objHeight, "Text", this::submit, "Tt");

    public Button back = new Button(this.centerX, this.centerY + this.objYSpace * 2, this.objWidth, this.objHeight, "Done", this::escape);

    public <T extends GameObject> OverlaySelectString(Screen previous, ScreenLevelEditor screenLevelEditor, StringSelector<T> selector)
    {
        super(previous, screenLevelEditor);

        screenLevelEditor.paused = true;
        this.selector = selector;
        this.textBox.inputText = selector.string.replaceAll("§", "\\\\u00a7");
        this.textBox.maxChars = 50;
        this.textBox.enableCaps = true;
        this.textBox.enablePunctuation = true;
    }

    @Override
    public void draw()
    {
        super.draw();

        Drawing.drawing.setColor(0, 0, 0);
        Drawing.drawing.setInterfaceFontSize(this.titleSize);
        Drawing.drawing.drawInterfaceText(this.centerX, this.centerY - this.objYSpace * 2, "Choose text");

        textBox.draw();
        back.draw();
    }

    @Override
    public void update()
    {
        textBox.update();
        back.update();
    }

    public void submit()
    {
        if (textBox.inputText.isEmpty() || textBox.inputText.equals(textBox.previousInputText))
        {
            textBox.inputText = textBox.previousInputText;
            return;
        }

        String text = textBox.inputText;
        if (text.matches(".*\\\\u00a7[\\d{12}].*"))
            text = text.replaceAll("\\\\u00a7", "§");

        this.selector.modified = true;
        this.selector.setMetadata(text);
    }
}
