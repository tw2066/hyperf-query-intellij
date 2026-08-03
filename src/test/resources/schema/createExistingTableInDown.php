<?php return new class extends Migration {
    public function up()
    {
         Schema::drop('users');
    }

    public function down()
    {
        Schema::create('users', function (\Hyperf\Database\Schema\Blueprint $table) {
            $table->string('<caret>');
        });
    }
}
