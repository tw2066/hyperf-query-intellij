<?php return new class extends Migration {
    public function up()
    {
        Schema::create('orders', function (\Hyperf\Database\Schema\Blueprint $table) {
              $table->id();
              $table->string('branch')->index();
              $table->integer('amount');
              $table->double('weight');
              $table->float('price');
              $table->timestamps();
          });

        Schema::table('orders', function (\Hyperf\Database\Schema\Blueprint $table) {
            $table->string('<caret>');
        })
    }
}
